package com.example.sanathanatattva.ui.screens.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.model.Product
import com.example.sanathanatattva.data.model.Review
import com.example.sanathanatattva.data.cart.CartLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val product: Product? = null,
    val imageList: List<String> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val averageRating: Double? = null,
    val reviewCount: Int = 0,
    val heldCount: Int = 0,
)

class ProductDetailViewModel(
    private val container: AppContainer,
    private val productId: Long,
) : ViewModel() {

    val state = MutableStateFlow(ProductDetailUiState())

    /** This product's lines in the cart, live — the page shows what's already added. */
    val cartLines: StateFlow<List<CartLine>> = container.cartStore.lines
        .map { lines -> lines.filter { it.productId == productId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        load()
    }

    fun load() {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            apiCall { container.api.product(productId) }
                .onSuccess { res ->
                    state.value = state.value.copy(
                        loading = false,
                        product = res.product,
                        imageList = parseImages(res.product),
                    )
                    loadExtras()
                }
                .onFailure { e ->
                    state.value = state.value.copy(loading = false, error = e.message)
                }
        }
    }

    private fun loadExtras() {
        viewModelScope.launch {
            apiCall { container.api.reviews(productId) }.onSuccess { res ->
                state.value = state.value.copy(
                    reviews = res.reviews,
                    averageRating = res.average_rating,
                    reviewCount = res.count,
                )
            }
            if (container.tokenStore.current() != null) {
                apiCall { container.api.refillCaps() }.onSuccess { res ->
                    state.value = state.value.copy(heldCount = res.caps[productId.toString()] ?: 0)
                }
            }
        }
    }

    fun addToCart(isRefill: Boolean) {
        val product = state.value.product ?: return
        viewModelScope.launch { container.cartStore.add(product, isRefill) }
    }

    fun removeFromCart(isRefill: Boolean) {
        viewModelScope.launch { container.cartStore.decrement(productId, isRefill) }
    }

    /** image_urls can be a JSON array, comma/newline separated, or absent. */
    private fun parseImages(product: Product): List<String> {
        val extra = product.image_urls?.let { raw ->
            runCatching {
                com.example.sanathanatattva.data.api.ApiClient.json
                    .decodeFromString<List<String>>(raw)
            }.getOrElse {
                raw.split(',', '\n').map(String::trim).filter { it.isNotBlank() }
            }
        } ?: emptyList()
        return (listOfNotNull(product.image_url) + extra).distinct()
    }
}
