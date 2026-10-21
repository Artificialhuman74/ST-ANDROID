package com.example.sanathanatattva.ui.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.cache.ApiCache
import com.example.sanathanatattva.data.cache.isFresh
import com.example.sanathanatattva.data.model.ContainerHolding
import com.example.sanathanatattva.data.model.DisputeRequest
import com.example.sanathanatattva.data.model.RefundRequest
import com.example.sanathanatattva.data.model.SwapRequest
import com.example.sanathanatattva.data.model.SwappableProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

data class ContainersUiState(
    val loading: Boolean = true,
    /** A pull-to-refresh is in flight (the list stays up meanwhile). */
    val refreshing: Boolean = false,
    val error: String? = null,
    val held: List<ContainerHolding> = emptyList(),
    val history: List<ContainerHolding> = emptyList(),
    val swappable: List<SwappableProduct> = emptyList(),
    val actionMessage: String? = null,
    val actionInProgress: Boolean = false,
    val rewardRoundActive: Boolean? = null,
)

/** What the tab shows, as saved on the phone between visits. */
@Serializable
data class ContainersSnapshot(
    val held: List<ContainerHolding> = emptyList(),
    val history: List<ContainerHolding> = emptyList(),
    val swappable: List<SwappableProduct> = emptyList(),
    val rewardRoundActive: Boolean? = null,
)

/**
 * Containers only change when an order is delivered or the customer acts on one, so the
 * tab opens from the copy saved on the phone and asks the server again after 3 hours.
 * Acting on a container, or pulling down, always refreshes.
 */
class ContainersViewModel(private val container: AppContainer) : ViewModel() {

    val state = MutableStateFlow(ContainersUiState())

    // Persisted flag: has the user seen the "Why steel?" welcome sheet?
    // null = still loading from DataStore (don't show sheet yet)
    val steelSheetSeen: StateFlow<Boolean?> = container.containerPrefs.steelSheetSeen
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    fun markSteelSheetSeen() {
        viewModelScope.launch { container.containerPrefs.markSteelSheetSeen() }
    }

    /** Whose containers are on screen, and when that answer came from the server. */
    private var shownFor: String? = null
    private var shownSavedAt = 0L

    /** Opening the tab: draws what's saved, and asks the server only once that's 3 hours old. */
    fun open() {
        viewModelScope.launch {
            val owner = ApiCache.ownerOf(container.tokenStore.current())
            if (owner == null) {
                shownFor = null
                state.value = ContainersUiState(loading = false)
                return@launch
            }
            if (owner == shownFor && isFresh(shownSavedAt)) return@launch
            if (owner != shownFor) {
                val saved = container.cache.read(ApiCache.CONTAINERS, ContainersSnapshot.serializer(), owner)
                if (saved == null) {
                    state.value = ContainersUiState()
                } else {
                    show(saved.value, owner, saved.savedAt)
                    if (saved.isFresh) return@launch
                }
            }
            fetch(owner, pulled = false)
        }
    }

    /** Asks the server now: retry, pull-to-refresh, and after any action on a container. */
    fun load(pulled: Boolean = false) {
        viewModelScope.launch {
            val owner = ApiCache.ownerOf(container.tokenStore.current())
            if (owner == null) {
                shownFor = null
                state.value = ContainersUiState(loading = false)
                return@launch
            }
            if (owner != shownFor) state.value = ContainersUiState()
            fetch(owner, pulled)
        }
    }

    private suspend fun fetch(owner: String, pulled: Boolean) {
        val s = state.value
        state.value = s.copy(
            loading = s.held.isEmpty() && s.history.isEmpty(),
            refreshing = pulled,
            error = null,
        )
        apiCall { container.api.containers() }
            .onSuccess { res ->
                val rewardRoundActive = if (res.held.any { it.source == "lucky_draw_reward" }) {
                    apiCall { container.api.luckyDrawStatus() }.getOrNull()?.let { it.enrollment != null }
                } else false
                val snapshot = ContainersSnapshot(res.held, res.history, res.swappable, rewardRoundActive)
                show(snapshot, owner, System.currentTimeMillis())
                container.cache.write(ApiCache.CONTAINERS, ContainersSnapshot.serializer(), snapshot, owner)
            }
            .onFailure { e ->
                // A failed refresh keeps the saved list up; the error page is only for
                // when there's nothing to show.
                val shown = state.value.held.isNotEmpty() || state.value.history.isNotEmpty()
                state.value = state.value.copy(
                    loading = false,
                    refreshing = false,
                    error = e.message.takeUnless { shown },
                )
            }
    }

    private fun show(snapshot: ContainersSnapshot, owner: String, savedAt: Long) {
        shownFor = owner
        shownSavedAt = savedAt
        state.value = state.value.copy(
            loading = false,
            refreshing = false,
            error = null,
            held = snapshot.held,
            history = snapshot.history,
            swappable = snapshot.swappable,
            rewardRoundActive = snapshot.rewardRoundActive,
        )
    }

    private fun runAction(successMessage: String, block: suspend () -> Unit) {
        if (state.value.actionInProgress) return
        state.value = state.value.copy(actionInProgress = true, actionMessage = null)
        viewModelScope.launch {
            apiCall { block() }
                .onSuccess {
                    state.value = state.value.copy(
                        actionInProgress = false,
                        actionMessage = successMessage,
                    )
                    load()
                }
                .onFailure { e ->
                    state.value = state.value.copy(
                        actionInProgress = false,
                        actionMessage = e.message,
                    )
                }
        }
    }

    fun requestRefund(holdingId: Long, destination: String) = runAction(
        "Refund requested. We'll arrange the container pickup.",
    ) { container.api.requestRefund(holdingId, RefundRequest(destination)) }

    fun cancelRefund(holdingId: Long) = runAction("Refund request cancelled.") {
        container.api.cancelRefund(holdingId)
    }

    fun swap(holdingId: Long, targetProductId: Long) = runAction(
        "Swap requested. Your next delivery will carry the new oil.",
    ) { container.api.swapContainer(holdingId, SwapRequest(targetProductId)) }

    fun dispute(holdingId: Long, notes: String) = runAction(
        "Dispute opened. Our team will contact you.",
    ) { container.api.disputeContainer(holdingId, DisputeRequest(notes.ifBlank { null })) }

    fun clearMessage() {
        state.value = state.value.copy(actionMessage = null)
    }
}
