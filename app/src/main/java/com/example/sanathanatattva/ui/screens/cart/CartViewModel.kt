package com.example.sanathanatattva.ui.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.cart.CartLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CartViewModel(private val container: AppContainer) : ViewModel() {

    val lines: StateFlow<List<CartLine>> = container.cartStore.lines
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** product_id -> held containers usable for refills. */
    val refillCaps = MutableStateFlow<Map<Long, Int>>(emptyMap())

    init {
        viewModelScope.launch {
            if (container.tokenStore.current() == null) return@launch
            apiCall { container.api.refillCaps() }.onSuccess { res ->
                refillCaps.value = res.caps.mapKeys { it.key.toLongOrNull() ?: -1L }
            }
        }
    }

    fun setQuantity(line: CartLine, quantity: Int) {
        viewModelScope.launch {
            container.cartStore.setQuantity(line.productId, line.isRefill, quantity)
        }
    }

    fun setRefill(line: CartLine, refill: Boolean) {
        viewModelScope.launch {
            container.cartStore.setRefill(line.productId, line.isRefill, refill)
        }
    }
}
