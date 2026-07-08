package com.example.sanathanatattva.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class OrdersUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val orders: List<Order> = emptyList(),
)

class OrdersViewModel(private val container: AppContainer) : ViewModel() {

    val state = MutableStateFlow(OrdersUiState())

    fun load() {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            if (container.tokenStore.current() == null) {
                state.value = OrdersUiState(loading = false)
                return@launch
            }
            apiCall { container.api.orders() }
                .onSuccess { res ->
                    state.value = OrdersUiState(loading = false, orders = res.orders)
                }
                .onFailure { e ->
                    state.value = state.value.copy(loading = false, error = e.message)
                }
        }
    }
}
