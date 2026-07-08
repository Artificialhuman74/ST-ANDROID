package com.example.sanathanatattva.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.model.Order
import com.example.sanathanatattva.data.model.OrderItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val order: Order? = null,
    val items: List<OrderItem> = emptyList(),
)

class OrderDetailViewModel(
    private val container: AppContainer,
    private val orderId: Long,
) : ViewModel() {

    val state = MutableStateFlow(OrderDetailUiState())

    init {
        load()
        // Light polling keeps the status timeline live while the screen is open.
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                val status = state.value.order?.status
                if (status != null && status != "delivered" && status != "cancelled") refresh()
            }
        }
    }

    fun load() {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        apiCall { container.api.order(orderId) }
            .onSuccess { res ->
                state.value = OrderDetailUiState(
                    loading = false,
                    order = res.order,
                    items = res.items.ifEmpty { res.order.items },
                )
            }
            .onFailure { e ->
                state.value = state.value.copy(loading = false, error = e.message)
            }
    }
}
