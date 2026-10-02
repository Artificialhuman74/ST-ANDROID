package com.example.sanathanatattva.ui.screens.luckydraw

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.data.api.ApiService
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.model.*
import com.example.sanathanatattva.data.prefs.DrawMark
import com.example.sanathanatattva.data.prefs.DrawSeenStore
import com.example.sanathanatattva.data.ApiException
import com.example.sanathanatattva.payments.PaymentEvent
import com.example.sanathanatattva.payments.PaymentEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

data class DrawPayment(val key: String, val orderId: String, val amount: Long, val currency: String)

data class LuckyDrawUiState(
    val loading: Boolean = true,
    val status: LuckyDrawStatus? = null,
    val consumer: Consumer? = null,
    val addresses: List<Address>? = null,
    val gift: Product? = null,
    val winners: List<DrawWinner> = emptyList(),
    val error: String? = null,
    val message: String? = null,
    val busy: String? = null,
    val unseenResult: Boolean = false,
    /** The home page's "someone else won" card has been dismissed for this draw. */
    val resultDismissed: Boolean = false,
    val verificationPending: Boolean = false,
    /** The month key of a claim this screen just placed, so the round rail can pour that month. */
    val justClaimed: String? = null,
    /** A failed claim, shown inline on the winner's card rather than as a page error. */
    val claimError: String? = null,
)

const val CLAIM_BUSY = "Placing free order…"

/** The server owns enrollment, eligibility, deadlines and claims on both platforms. */
class LuckyDrawViewModel(
    private val api: ApiService,
    private val prefs: DrawSeenStore,
    private val saved: SavedStateHandle,
    private val consumerId: Long,
) : ViewModel() {
    val state = MutableStateFlow(LuckyDrawUiState())
    private val paymentChannel = Channel<DrawPayment>(Channel.BUFFERED)
    val payments = paymentChannel.receiveAsFlow()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            PaymentEvents.events.collect { event ->
                val expectedOrder = saved.get<String>("draw_order") ?: return@collect
                when (event) {
                    is PaymentEvent.Success -> {
                        // Other checkout screens share the Activity's callbacks.
                        if (event.orderId != null && event.orderId != expectedOrder) return@collect
                        if (state.value.busy == "Confirming payment…") return@collect
                        if (event.orderId.isNullOrBlank() || event.signature.isNullOrBlank() || event.paymentId.isBlank()) {
                            state.value = state.value.copy(busy = null, error =
                                "Payment details were incomplete. Refresh to check your enrollment before paying again.")
                            refresh(details = true)
                            return@collect
                        }
                        saved["draw_payment"] = event.paymentId
                        saved["draw_signature"] = event.signature
                        verifyPayment()
                    }
                    is PaymentEvent.Failure -> if (state.value.busy == "Waiting for payment…") {
                        paymentDismissed()
                    }
                }
            }
        }
    }

    fun refresh(details: Boolean = false) {
        if (refreshJob?.isActive == true || state.value.busy != null) return
        state.value = state.value.copy(loading = true)
        refreshJob = viewModelScope.launch {
            val result = drawCall { api.luckyDrawStatus() }
            result.onSuccess { status ->
                val cycle = status.cycle
                val draw = cycle?.latest_draw
                val unseen = cycle != null && draw != null && belongsToEnrollment(draw, status.enrollment) &&
                    !seen(cycle.scheme_id, draw.month_key, DrawMark.REVEAL, ifUnknown = true)
                val dismissed = cycle != null && draw != null && seen(cycle.scheme_id, draw.month_key, DrawMark.RESULT_INFO, ifUnknown = false)
                val recovered = saved.get<Long>("draw_enrollment") == status.enrollment?.id && status.enrollment != null
                if (recovered) clearPending()
                state.value = state.value.copy(
                    status = status, error = null, unseenResult = unseen, resultDismissed = dismissed,
                    verificationPending = saved.get<String>("draw_payment") != null,
                    message = if (recovered) "Your enrollment is confirmed." else state.value.message,
                )
                if (details) {
                    val profile = async { drawCall { api.me() } }
                    val addresses = async { drawCall { api.addresses() } }
                    val gifts = async { drawCall { api.luckyDrawGifts() } }
                    val winners = async { drawCall { api.luckyDrawWinners() } }
                    val profileResult = profile.await()
                    val addressResult = addresses.await()
                    val giftResult = gifts.await().getOrNull()
                    state.value = state.value.copy(
                        consumer = profileResult.getOrNull()?.consumer,
                        addresses = addressResult.getOrNull()?.addresses,
                        gift = giftResult?.product ?: giftResult?.products?.firstOrNull(),
                        winners = winners.await().getOrNull()?.winners.orEmpty(),
                        error = profileResult.exceptionOrNull()?.message ?: addressResult.exceptionOrNull()?.message,
                    )
                }
            }.onFailure { state.value = state.value.copy(error = it.message ?: "Couldn't load the draw.") }
            state.value = state.value.copy(loading = false)
        }
    }

    fun enroll(scheme: DrawScheme, wantsGift: Boolean) {
        val current = state.value
        if (current.busy != null || current.loading || current.verificationPending || current.status?.enrollment != null) return
        if (current.consumer?.linked_dealer_id == null || current.addresses.isNullOrEmpty()) return
        state.value = current.copy(busy = "Starting enrollment…", error = null, message = null)
        viewModelScope.launch {
            // The GET reconciles a payment whose callback was lost before another charge is started.
            val latest = drawCall { api.luckyDrawStatus() }
            if (latest.isFailure || latest.getOrNull()?.enrollment != null) {
                state.value = state.value.copy(busy = null, error = latest.exceptionOrNull()?.message)
                refresh(details = true)
                return@launch
            }
            drawCall { api.enrollLuckyDraw(EnrollDrawRequest(scheme.id, wantsGift)) }
                .onSuccess { response ->
                    if (response.mocked) {
                        joined(response.order_number, response.welcome_gift)
                    } else {
                        val id = response.enrollment_id
                        val order = response.razorpay_order_id
                        val key = response.key_id
                        val amount = response.amount
                        if (id == null || order.isNullOrBlank() || key.isNullOrBlank() || amount == null || amount <= 0) {
                            state.value = state.value.copy(busy = null, error = "Couldn't start payment. Refresh and try again.")
                        } else {
                            saved["draw_enrollment"] = id
                            saved["draw_order"] = order
                            state.value = state.value.copy(busy = "Waiting for payment…")
                            paymentChannel.send(DrawPayment(key, order, amount, response.currency))
                        }
                    }
                }.onFailure { state.value = state.value.copy(busy = null, error = it.message) }
        }
    }

    fun verifyPayment() {
        val id = saved.get<Long>("draw_enrollment") ?: return
        val order = saved.get<String>("draw_order") ?: return
        val payment = saved.get<String>("draw_payment") ?: return
        val signature = saved.get<String>("draw_signature") ?: return
        if (state.value.busy == "Confirming payment…") return
        refreshJob?.cancel()
        state.value = state.value.copy(busy = "Confirming payment…", loading = false, error = null)
        viewModelScope.launch {
            drawCall { api.verifyLuckyDraw(VerifyDrawRequest(id, order, payment, signature)) }
                .onSuccess { joined(it.order_number, it.welcome_gift) }
                .onFailure {
                    state.value = state.value.copy(busy = null, verificationPending = true,
                        error = "Payment $payment was received, but enrollment could not be confirmed. " +
                            "Retry confirmation or refresh. If this persists, contact support with this payment ID.")
                }
        }
    }

    private fun joined(orderNumber: String?, gift: String?) {
        clearPending()
        state.value = state.value.copy(busy = null, verificationPending = false, error = null,
            message = if (orderNumber != null) "You're enrolled! ${gift ?: "Your welcome gift"} is order $orderNumber."
                else "You're enrolled! Make a qualifying purchase to enter the draw.")
        refresh(details = true)
    }

    private fun clearPending() {
        for (key in listOf("draw_enrollment", "draw_order", "draw_payment", "draw_signature")) saved.remove<Any>(key)
    }

    fun paymentDismissed() {
        state.value = state.value.copy(busy = null,
            message = "Payment cancelled or not completed. Checking your enrollment; you can retry if it wasn't paid.")
        refresh(details = true)
    }

    fun checkPaymentStatus() {
        if (state.value.busy != "Waiting for payment…") return
        state.value = state.value.copy(busy = null, message = "Checking payment confirmation…")
        refresh(details = true)
    }

    fun claimReward() {
        if (state.value.busy != null || state.value.loading || state.value.status?.cycle?.canClaim != true) return
        state.value = state.value.copy(busy = CLAIM_BUSY, error = null, message = null, claimError = null)
        viewModelScope.launch {
            val result = drawCall { api.claimLuckyDrawReward() }
            state.value = state.value.copy(
                busy = null,
                justClaimed = if (result.isSuccess) state.value.status?.cycle?.month_key else state.value.justClaimed,
                message = result.getOrNull()?.let {
                    "Your free delivery is ordered${it.order_number?.let { n -> ": $n" }.orEmpty()}. Track it in Orders."
                },
                claimError = result.exceptionOrNull()?.let { it.message ?: "That didn't go through. Try again in a moment." },
            )
            // A duplicate claim from another device must also update this device's button.
            if (result.isSuccess || (result.exceptionOrNull() as? ApiException)?.code == 409) refresh(details = true)
        }
    }

    fun linkDealer(code: String) {
        if (state.value.busy != null || state.value.loading || code.isBlank()) return
        action("Linking trader…") {
            api.linkDealer(LinkDealerRequest(code.trim().uppercase(java.util.Locale.ROOT)))
            "Your trader is linked. You can now join the round."
        }
    }

    fun leave() {
        if (state.value.busy != null || state.value.loading || state.value.status?.enrollment == null) return
        action("Leaving the round…") {
            val result = api.leaveLuckyDraw()
            "You have left ${result.scheme_name ?: "the round"}." +
                if (result.containers_returned > 0) " Your trader will collect your reward container." else ""
        }
    }

    private fun action(label: String, block: suspend () -> String) {
        state.value = state.value.copy(busy = label, error = null, message = null)
        viewModelScope.launch {
            val result = drawCall(block)
            state.value = state.value.copy(busy = null, message = result.getOrNull(), error = result.exceptionOrNull()?.message)
            // A duplicate claim from another device must also update this device's button.
            if (result.isSuccess || (result.exceptionOrNull() as? ApiException)?.code == 409) refresh(details = true)
        }
    }

    /** "Got it" on the home page's result card: it stays gone for this draw. */
    fun dismissResult() {
        val cycle = state.value.status?.cycle ?: return
        val draw = cycle.latest_draw ?: return
        state.value = state.value.copy(resultDismissed = true)
        viewModelScope.launch {
            try { prefs.markSeen(consumerId, cycle.scheme_id, draw.month_key, DrawMark.RESULT_INFO) } catch (_: java.io.IOException) { }
        }
    }

    /**
     * True exactly once per scheme and month: the first time this account is shown as in
     * this month's draw. The home row marks that crossing with a single gold pass.
     */
    suspend fun firstLookAtEntry(cycle: DrawCycle): Boolean {
        if (!enrolledLine(cycle).inThisDraw) return false
        if (seen(cycle.scheme_id, cycle.month_key, DrawMark.ENTERED, ifUnknown = true)) return false
        return try {
            prefs.markSeen(consumerId, cycle.scheme_id, cycle.month_key, DrawMark.ENTERED)
            true
        } catch (_: java.io.IOException) { false }
    }

    private suspend fun seen(schemeId: Long, month: String, mark: DrawMark, ifUnknown: Boolean): Boolean = try {
        prefs.hasSeen(consumerId, schemeId, month, mark)
    } catch (_: java.io.IOException) { ifUnknown }

    fun acknowledgeResult() {
        val cycle = state.value.status?.cycle ?: return
        val draw = cycle.latest_draw ?: return
        viewModelScope.launch {
            try {
                prefs.markSeen(consumerId, cycle.scheme_id, draw.month_key)
                state.value = state.value.copy(unseenResult = false)
            } catch (_: java.io.IOException) {
                state.value = state.value.copy(unseenResult = false)
            }
        }
    }
}

private suspend fun <T> drawCall(block: suspend () -> T): Result<T> = try {
    apiCall(block)
} catch (e: CancellationException) {
    throw e
} catch (_: SerializationException) {
    Result.failure(IllegalStateException("The draw response couldn't be read. Please refresh or try again later."))
}
