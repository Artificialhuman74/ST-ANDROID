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
    val sort: ShopSort = ShopSort.Featured,
    /** product_id -> held container count usable for refills. */
    val refillCaps: Map<Long, Int> = emptyMap(),
)

enum class ShopSort(val label: String) {
    Featured("Featured"),
    PriceLow("Price: low to high"),
    PriceHigh("Price: high to low"),
    Name("Name A–Z"),
}

/**
 * What the list shows. The catalogue is small, so search, category and sort all
 * run on the device: results change as you type, with no reload from the server.
 */
fun ShopUiState.visibleProducts(): List<Product> {
    val q = search.trim().lowercase()
    var list = products.filter { p ->
        (selectedCategory == null || p.category == selectedCategory) &&
            (q.isEmpty() || p.name.lowercase().contains(q) || p.description.orEmpty().lowercase().contains(q))
    }
    if (q.isNotEmpty()) {
        // Names starting with what you typed first, then name matches, then description-only.
        list = list.sortedBy { p ->
            val n = p.name.lowercase()
            if (n.startsWith(q)) 0 else if (n.contains(q)) 1 else 2
        }
    }
    return when (sort) {
        ShopSort.Featured -> list
        ShopSort.PriceLow -> list.sortedBy { it.price }
        ShopSort.PriceHigh -> list.sortedByDescending { it.price }
        ShopSort.Name -> list.sortedBy { it.name.lowercase() }
    }
}

class ShopViewModel(private val container: AppContainer) : ViewModel() {

    val state = MutableStateFlow(ShopUiState())

    val cartLines: StateFlow<List<CartLine>> = container.cartStore.lines
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        load()
    }

    /** Loads the whole catalogue once; filtering happens locally (see [visibleProducts]). */
    fun load() {
        state.value = state.value.copy(loading = state.value.products.isEmpty(), error = null)
        viewModelScope.launch {
            apiCall { container.api.products(search = null, category = null) }
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

    fun selectCategory(category: String?) {
        state.value = state.value.copy(selectedCategory = category)
    }

    fun setSort(sort: ShopSort) {
        state.value = state.value.copy(sort = sort)
    }

    fun addToCart(product: Product, isRefill: Boolean) {
        viewModelScope.launch { container.cartStore.add(product, isRefill) }
    }

    /** Removes one unit of a cart line; drops the line entirely when it hits zero. */
    fun removeFromCart(productId: Long, isRefill: Boolean) {
        viewModelScope.launch { container.cartStore.decrement(productId, isRefill) }
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
