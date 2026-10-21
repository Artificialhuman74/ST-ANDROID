package com.example.sanathanatattva.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.model.GeoPoint
import com.example.sanathanatattva.data.model.Order
import com.example.sanathanatattva.data.model.OrderItem
import com.example.sanathanatattva.data.model.OrderTracking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class OrderDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val order: Order? = null,
    val items: List<OrderItem> = emptyList(),
    /** Live tracking; null until it loads, or if this server doesn't have it yet. */
    val tracking: OrderTracking? = null,
    /** When [tracking] last arrived (ms), for "Updated just now". */
    val trackedAt: Long = 0L,
    /** Road still to go for the delivery partner, from our OSRM. */
    val route: List<GeoPoint> = emptyList(),
    val calling: Boolean = false,
)

class OrderDetailViewModel(
    private val container: AppContainer,
    private val orderId: Long,
) : ViewModel() {

    val state = MutableStateFlow(OrderDetailUiState())

    /** One-shot: a number to dial, or a message to show. */
    val callEvents = MutableStateFlow<CallEvent?>(null)

    private var routeKey: String? = null

    init {
        load()
        // Live while the screen is open: every 10 s while someone is on the move with it,
        // every 30 s before that, and not at all once it's delivered or cancelled.
        viewModelScope.launch {
            while (isActive) {
                delay(if (state.value.tracking?.delivery_partner?.live != null) 10_000 else 30_000)
                if (!finished()) refresh()
            }
        }
    }

    fun load() {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch { refresh(withOrder = true) }
    }

    private fun finished(): Boolean {
        val s = state.value
        val t = s.tracking?.state
        return t == "delivered" || t == "cancelled" ||
            (t == null && (s.order?.status == "delivered" || s.order?.status == "cancelled"))
    }

    private suspend fun refresh(withOrder: Boolean = false) {
        val before = state.value.tracking?.state
        apiCall { container.api.tracking(orderId) }.onSuccess { t ->
            state.value = state.value.copy(tracking = t, trackedAt = System.currentTimeMillis())
            loadRoute(t)
        }
        // The order itself (items, bill) changes only when its stage does.
        if (!withOrder && state.value.order != null && before == state.value.tracking?.state) return
        apiCall { container.api.order(orderId) }
            .onSuccess { res ->
                state.value = state.value.copy(
                    loading = false,
                    error = null,
                    order = res.order,
                    items = res.items.ifEmpty { res.order.items },
                )
            }
            .onFailure { e ->
                state.value = state.value.copy(loading = false, error = e.message.takeIf { state.value.order == null })
            }
    }

    /** Road from the delivery partner to the door; refetched only after they've moved ~100 m. */
    private suspend fun loadRoute(t: OrderTracking) {
        val from = t.delivery_partner?.live
        val to = t.home
        if (from == null || to == null) {
            routeKey = null
            if (state.value.route.isNotEmpty()) state.value = state.value.copy(route = emptyList())
            return
        }
        fun r(v: Double) = String.format(Locale.ROOT, "%.3f", v)
        val key = "${r(from.lat)},${r(from.lng)}|${r(to.lat)},${r(to.lng)}"
        if (key == routeKey) return
        apiCall {
            container.api.route(
                from = String.format(Locale.ROOT, "%.5f,%.5f", from.lat, from.lng),
                to = String.format(Locale.ROOT, "%.5f,%.5f", to.lat, to.lng),
            )
        }.onSuccess { res ->
            routeKey = key
            state.value = state.value.copy(
                route = res.points.mapNotNull { p -> if (p.size >= 2) GeoPoint(p[0], p[1]) else null },
            )
        }
    }

    /** The number is asked for only when tapped, and only works while it's on its way. */
    fun call() {
        if (state.value.calling) return
        state.value = state.value.copy(calling = true)
        viewModelScope.launch {
            apiCall { container.api.callNumber(orderId) }
                .onSuccess { callEvents.value = CallEvent.Dial(it.phone) }
                .onFailure { callEvents.value = CallEvent.Failed(it.message ?: "Couldn't get the number") }
            state.value = state.value.copy(calling = false)
        }
    }

    fun consumeCall() {
        callEvents.value = null
    }
}

sealed interface CallEvent {
    data class Dial(val phone: String) : CallEvent
    data class Failed(val message: String) : CallEvent
}
