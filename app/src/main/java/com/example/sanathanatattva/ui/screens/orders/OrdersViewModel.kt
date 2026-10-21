package com.example.sanathanatattva.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.cache.ApiCache
import com.example.sanathanatattva.data.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class OrdersUiState(
    val loading: Boolean = true,
    /** A pull-to-refresh is in flight (the list stays up meanwhile). */
    val refreshing: Boolean = false,
    val error: String? = null,
    val orders: List<Order> = emptyList(),
)

/**
 * Orders move while the customer watches (packed, out for delivery, delivered), so the
 * tab asks the server every time it opens. The last list stays on screen meanwhile and
 * updates in place; only a first load shows placeholders.
 */
class OrdersViewModel(private val container: AppContainer) : ViewModel() {

    val state = MutableStateFlow(OrdersUiState())

    /** Whose orders are on screen; another login never sees them while its own load. */
    private var shownFor: String? = null

    fun load(pulled: Boolean = false) {
        viewModelScope.launch {
            val owner = ApiCache.ownerOf(container.tokenStore.current())
            if (owner == null) {
                shownFor = null
                state.value = OrdersUiState(loading = false)
                return@launch
            }
            if (owner != shownFor) state.value = OrdersUiState()
            state.value = state.value.copy(
                loading = state.value.orders.isEmpty(),
                refreshing = pulled,
                error = null,
            )
            apiCall { container.api.orders() }
                .onSuccess { res ->
                    shownFor = owner
                    state.value = OrdersUiState(loading = false, orders = res.orders)
                }
                .onFailure { e ->
                    state.value = state.value.copy(loading = false, refreshing = false, error = e.message)
                }
        }
    }
}
