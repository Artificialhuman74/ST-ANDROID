package com.example.sanathanatattva.ui.screens.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
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

data class ContainersUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val held: List<ContainerHolding> = emptyList(),
    val history: List<ContainerHolding> = emptyList(),
    val swappable: List<SwappableProduct> = emptyList(),
    val actionMessage: String? = null,
    val actionInProgress: Boolean = false,
)

class ContainersViewModel(private val container: AppContainer) : ViewModel() {

    val state = MutableStateFlow(ContainersUiState())

    // Persisted flag: has the user seen the "Why steel?" welcome sheet?
    // null = still loading from DataStore (don't show sheet yet)
    val steelSheetSeen: StateFlow<Boolean?> = container.containerPrefs.steelSheetSeen
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    fun markSteelSheetSeen() {
        viewModelScope.launch { container.containerPrefs.markSteelSheetSeen() }
    }

    fun load() {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            if (container.tokenStore.current() == null) {
                state.value = ContainersUiState(loading = false)
                return@launch
            }
            apiCall { container.api.containers() }
                .onSuccess { res ->
                    state.value = state.value.copy(
                        loading = false,
                        held = res.held,
                        history = res.history,
                        swappable = res.swappable,
                    )
                }
                .onFailure { e ->
                    state.value = state.value.copy(loading = false, error = e.message)
                }
        }
    }

    private fun runAction(successMessage: String, block: suspend () -> Unit) {
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
