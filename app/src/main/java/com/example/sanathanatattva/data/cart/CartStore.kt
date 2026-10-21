package com.example.sanathanatattva.data.cart

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.sanathanatattva.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.cartDataStore by preferencesDataStore(name = "cart")

/**
 * A cart line. `isRefill = true` means the customer pours into a container
 * they already hold, so no fresh deposit is charged.
 */
@Serializable
data class CartLine(
    val productId: Long,
    val name: String,
    val unit: String,
    val price: Double,
    val containerCost: Double,
    val containerType: String?,
    val imageUrl: String?,
    val quantity: Int,
    val isRefill: Boolean,
)

data class CartTotals(
    val itemCount: Int,
    val subtotal: Double,
    val depositTotal: Double,
) {
    val payable: Double get() = subtotal + depositTotal
}

fun List<CartLine>.totals() = CartTotals(
    itemCount = sumOf { it.quantity },
    subtotal = sumOf { it.price * it.quantity },
    depositTotal = sumOf { if (it.isRefill) 0.0 else it.containerCost * it.quantity },
)

/** Persists the cart across app restarts, like the web app's localStorage cart. */
class CartStore(private val context: Context) {

    private val cartKey = stringPreferencesKey("cart_lines")
    private val json = Json { ignoreUnknownKeys = true }

    val lines: Flow<List<CartLine>> = context.cartDataStore.data.map { prefs -> decode(prefs[cartKey]) }

    suspend fun current(): List<CartLine> = lines.first()

    private fun decode(raw: String?): List<CartLine> =
        raw?.let { runCatching { json.decodeFromString<List<CartLine>>(it) }.getOrDefault(emptyList()) } ?: emptyList()

    /**
     * Read-modify-write inside one DataStore transaction, so quick repeated taps
     * on + never read the same old cart and drop an increment.
     */
    private suspend fun update(change: (MutableList<CartLine>) -> Unit) {
        context.cartDataStore.edit { prefs ->
            val lines = decode(prefs[cartKey]).toMutableList()
            change(lines)
            prefs[cartKey] = json.encodeToString(lines.toList())
        }
    }

    /** Adds one unit of the product. Buy and refill are separate lines. */
    suspend fun add(product: Product, isRefill: Boolean) = update { lines ->
        val idx = lines.indexOfFirst { it.productId == product.id && it.isRefill == isRefill }
        if (idx >= 0) {
            lines[idx] = lines[idx].copy(quantity = lines[idx].quantity + 1)
        } else {
            lines += CartLine(
                productId = product.id,
                name = product.name,
                unit = product.unit,
                price = product.price,
                containerCost = product.container_cost ?: 0.0,
                containerType = product.container_type,
                imageUrl = product.image_url,
                quantity = 1,
                isRefill = isRefill,
            )
        }
    }

    suspend fun setQuantity(productId: Long, isRefill: Boolean, quantity: Int) = update { lines ->
        val idx = lines.indexOfFirst { it.productId == productId && it.isRefill == isRefill }
        if (idx >= 0) {
            if (quantity <= 0) lines.removeAt(idx)
            else lines[idx] = lines[idx].copy(quantity = quantity)
        }
    }

    /** Takes one unit off a line atomically; drops the line at zero. */
    suspend fun decrement(productId: Long, isRefill: Boolean) = update { lines ->
        val idx = lines.indexOfFirst { it.productId == productId && it.isRefill == isRefill }
        if (idx >= 0) {
            val q = lines[idx].quantity - 1
            if (q <= 0) lines.removeAt(idx) else lines[idx] = lines[idx].copy(quantity = q)
        }
    }

    /** Switches a line between buy and refill, merging with an existing counterpart line. */
    suspend fun setRefill(productId: Long, fromRefill: Boolean, toRefill: Boolean) {
        if (fromRefill == toRefill) return
        update { lines -> moveLine(lines, productId, fromRefill, toRefill) }
    }

    private fun moveLine(lines: MutableList<CartLine>, productId: Long, fromRefill: Boolean, toRefill: Boolean) {
        val idx = lines.indexOfFirst { it.productId == productId && it.isRefill == fromRefill }
        if (idx < 0) return
        val moving = lines.removeAt(idx)
        val targetIdx = lines.indexOfFirst { it.productId == productId && it.isRefill == toRefill }
        if (targetIdx >= 0) {
            lines[targetIdx] = lines[targetIdx].copy(quantity = lines[targetIdx].quantity + moving.quantity)
        } else {
            lines += moving.copy(isRefill = toRefill)
        }
    }

    suspend fun clear() {
        context.cartDataStore.edit { it.remove(cartKey) }
    }
}
