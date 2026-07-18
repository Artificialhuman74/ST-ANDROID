package com.example.sanathanatattva.ui.screens.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.cart.CartLine
import com.example.sanathanatattva.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShopUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val products: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val search: String = "",
    /** product_id -> held container count usable for refills. */
    val refillCaps: Map<Long, Int> = emptyMap(),
)

class ShopViewModel(private val container: AppContainer) : ViewModel() {

    val state = MutableStateFlow(ShopUiState())

    val cartLines: StateFlow<List<CartLine>> = container.cartStore.lines
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        load()
    }

    fun load() {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            apiCall {
                container.api.products(
                    search = state.value.search.ifBlank { null },
                    category = state.value.selectedCategory,
                )
            }
                .onSuccess { res ->
                    state.value = state.value.copy(
                        loading = false,
                        products = res.products,
                        categories = res.categories,
                    )
                }
                .onFailure { e ->
                    state.value = state.value.copy(loading = false, error = e.message)
                }
            refreshCaps()
        }
    }

    fun refreshCaps() {
        viewModelScope.launch {
            if (container.tokenStore.current() == null) return@launch
            apiCall { container.api.refillCaps() }.onSuccess { res ->
                state.value = state.value.copy(
                    refillCaps = res.caps.mapKeys { it.key.toLongOrNull() ?: -1L },
                )
            }
        }
    }

    fun setSearch(text: String) {
        state.value = state.value.copy(search = text)
    }

    fun submitSearch() = load()

    fun selectCategory(category: String?) {
        state.value = state.value.copy(selectedCategory = category)
        load()
    }

    fun addToCart(product: Product, isRefill: Boolean) {
        viewModelScope.launch { container.cartStore.add(product, isRefill) }
    }

    /** Removes one unit of a cart line; drops the line entirely when it hits zero. */
    fun removeFromCart(productId: Long, isRefill: Boolean) {
        val line = cartLines.value
            .firstOrNull { it.productId == productId && it.isRefill == isRefill } ?: return
        viewModelScope.launch {
            container.cartStore.setQuantity(productId, isRefill, line.quantity - 1)
        }
    }

    /** Held containers not yet spoken for by refill lines already in the cart. */
    fun refillRemaining(productId: Long): Int {
        val held = state.value.refillCaps[productId] ?: 0
        val inCart = cartLines.value
            .filter { it.productId == productId && it.isRefill }
            .sumOf { it.quantity }
        return (held - inCart).coerceAtLeast(0)
    }
}
