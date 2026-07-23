package com.example.sanathanatattva.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.cart.CartLine
import com.example.sanathanatattva.data.cart.totals
import com.example.sanathanatattva.data.model.Address
import com.example.sanathanatattva.data.model.Consumer
import com.example.sanathanatattva.data.model.CreatePaymentRequest
import com.example.sanathanatattva.data.model.OrderItemRequest
import com.example.sanathanatattva.data.model.PlaceOrderRequest
import com.example.sanathanatattva.data.model.VerifyPaymentRequest
import com.example.sanathanatattva.payments.PaymentEvent
import com.example.sanathanatattva.payments.PaymentEvents
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** What the screen needs to hand Razorpay. */
data class RazorpayPayload(
    val keyId: String,
    val razorpayOrderId: String,
    val amountPaise: Long,
    val currency: String,
    val orderNumber: String,
    val name: String?,
    val email: String?,
    val phone: String?,
)

data class InlineAddress(
    val label: String = "Home",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val pincode: String = "",
    val save: Boolean = true,
)

sealed interface CheckoutStage {
    data object Form : CheckoutStage
    data object PlacingOrder : CheckoutStage
    data object AwaitingPayment : CheckoutStage
    data object Verifying : CheckoutStage
    data class Success(
        val orderId: Long,
        val orderNumber: String,
        val dealerName: String?,
        val dealerPhone: String?,
        val message: String?,
    ) : CheckoutStage
}

data class CheckoutUiState(
    val stage: CheckoutStage = CheckoutStage.Form,
    val error: String? = null,
    val consumer: Consumer? = null,
    val addresses: List<Address> = emptyList(),
    val selectedAddressId: Long? = null,
    val useInlineAddress: Boolean = false,
    val inline: InlineAddress = InlineAddress(),
    val referralCode: String = "",
    val referralDealerName: String? = null,
    val referralChecking: Boolean = false,
    val referralDiscountPercent: Double = 0.0,
    val walletBalance: Double = 0.0,
    val useWalletCredit: Boolean = false,
    val notes: String = "",
    val loading: Boolean = true,
)

class CheckoutViewModel(private val container: AppContainer) : ViewModel() {

    val state = MutableStateFlow(CheckoutUiState())
    val openRazorpay = MutableSharedFlow<RazorpayPayload>(extraBufferCapacity = 1)

    private var cart: List<CartLine> = emptyList()
    private var pendingOrderId: Long? = null
    private var pendingOrderNumber: String? = null
    private var pendingDelivery: Triple<String?, String?, String?>? = null // name, phone, message
    private var referralJob: Job? = null

    init {
        load()
        listenForPaymentResults()
    }

    private fun load() {
        viewModelScope.launch {
            cart = container.cartStore.current()
            val profile = apiCall { container.api.me() }.getOrNull()?.consumer
            val addresses = apiCall { container.api.addresses() }.getOrNull()?.addresses ?: emptyList()
            val wallet = apiCall { container.api.storeCredit() }.getOrNull()
            val settings = apiCall { container.api.settings() }.getOrNull()

            val defaultAddress = addresses.firstOrNull { it.is_default == 1 } ?: addresses.firstOrNull()
            val existingReferral = profile?.referral_code_used.orEmpty()

            state.value = state.value.copy(
                loading = false,
                consumer = profile,
                addresses = addresses,
                selectedAddressId = defaultAddress?.id,
                useInlineAddress = addresses.isEmpty(),
                referralCode = existingReferral,
                referralDealerName = profile?.dealer?.name,
                referralDiscountPercent = settings?.referral_discount_percent ?: 0.0,
                walletBalance = wallet?.balance ?: 0.0,
                inline = state.value.inline.copy(
                    name = profile?.name.orEmpty(),
                    phone = profile?.phone.orEmpty(),
                ),
            )
        }
    }

    private fun listenForPaymentResults() {
        viewModelScope.launch {
            PaymentEvents.events.collect { event ->
                val orderId = pendingOrderId ?: return@collect
                when (event) {
                    is PaymentEvent.Success -> {
                        state.value = state.value.copy(stage = CheckoutStage.Verifying)
                        val rzpOrderId = event.orderId
                        val signature = event.signature
                        if (rzpOrderId == null || signature == null) {
                            failPayment("Payment finished but details were incomplete. If money was deducted it will be refunded automatically.")
                            return@collect
                        }
                        apiCall {
                            container.api.verifyPayment(
                                VerifyPaymentRequest(
                                    razorpay_order_id = rzpOrderId,
                                    razorpay_payment_id = event.paymentId,
                                    razorpay_signature = signature,
                                    consumer_order_id = orderId,
                                )
                            )
                        }
                            .onSuccess {
                                container.cartStore.clear()
                                val (name, phone, message) = pendingDelivery
                                    ?: Triple(null, null, null)
                                state.value = state.value.copy(
                                    stage = CheckoutStage.Success(
                                        orderId = orderId,
                                        orderNumber = pendingOrderNumber ?: "",
                                        dealerName = name,
                                        dealerPhone = phone,
                                        message = message,
                                    ),
                                )
                            }
                            .onFailure { e ->
                                failPayment(
                                    "We couldn't confirm the payment: ${e.message} " +
                                        "If money was deducted it will be refunded automatically."
                                )
                            }
                    }

                    is PaymentEvent.Failure -> {
                        failPayment(humanizePaymentError(event.description))
                    }
                }
            }
        }
    }

    private suspend fun failPayment(message: String) {
        // Clean up the unpaid order so stock is released, like the web checkout does.
        pendingOrderId?.let { id ->
            apiCall { container.api.deleteUnpaidOrder(id) }
        }
        pendingOrderId = null
        pendingOrderNumber = null
        state.value = state.value.copy(stage = CheckoutStage.Form, error = message)
    }

    // ---- Form field updates ----

    fun selectAddress(id: Long) {
        state.value = state.value.copy(selectedAddressId = id, useInlineAddress = false)
    }

    fun useInlineAddress(use: Boolean) {
        state.value = state.value.copy(useInlineAddress = use)
    }

    fun updateInline(inline: InlineAddress) {
        state.value = state.value.copy(inline = inline)
    }

    fun setNotes(notes: String) {
        state.value = state.value.copy(notes = notes)
    }

    fun setUseWalletCredit(use: Boolean) {
        state.value = state.value.copy(useWalletCredit = use)
    }

    fun setReferralCode(code: String) {
        state.value = state.value.copy(referralCode = code.uppercase())
        referralJob?.cancel()
        if (code.isBlank()) {
            state.value = state.value.copy(referralDealerName = null, referralChecking = false)
            return
        }
        referralJob = viewModelScope.launch {
            state.value = state.value.copy(referralChecking = true)
            delay(450)
            apiCall { container.api.validateDealer(code.trim()) }
                .onSuccess { res ->
                    state.value = state.value.copy(
                        referralChecking = false,
                        referralDealerName = if (res.valid) res.dealerName else null,
                    )
                }
                .onFailure {
                    state.value = state.value.copy(referralChecking = false, referralDealerName = null)
                }
        }
    }

    // ---- Totals estimate (backend recomputes authoritatively) ----

    data class Estimate(
        val subtotal: Double,
        val deposits: Double,
        val discount: Double,
        val creditApplied: Double,
        val total: Double,
    )

    fun estimate(): Estimate {
        val t = cart.totals()
        val s = state.value
        val discount =
            if (s.referralDealerName != null && s.referralCode.isNotBlank())
                t.subtotal * s.referralDiscountPercent / 100.0
            else 0.0
        val beforeCredit = ceil(t.subtotal - discount + t.depositTotal)
        val credit =
            if (s.useWalletCredit) min(s.walletBalance, max(beforeCredit - 1.0, 0.0)) else 0.0
        return Estimate(
            subtotal = t.subtotal,
            deposits = t.depositTotal,
            discount = discount,
            creditApplied = credit,
            total = max(beforeCredit - credit, 1.0),
        )
    }

    fun cartLines(): List<CartLine> = cart

    // ---- Place order + pay ----

    fun placeOrderAndPay() {
        val s = state.value
        if (cart.isEmpty()) {
            state.value = s.copy(error = "Your cart is empty.")
            return
        }
        if (!s.useInlineAddress && s.selectedAddressId == null) {
            state.value = s.copy(error = "Choose a delivery address.")
            return
        }
        if (s.useInlineAddress) {
            val inline = s.inline
            if (inline.name.isBlank() || inline.phone.isBlank() ||
                inline.address.isBlank() || inline.pincode.isBlank()
            ) {
                state.value = s.copy(error = "Fill in the delivery name, phone, address and PIN code.")
                return
            }
        }

        state.value = s.copy(stage = CheckoutStage.PlacingOrder, error = null)
        viewModelScope.launch {
            val est = estimate()
            val request = PlaceOrderRequest(
                items = cart.map {
                    OrderItemRequest(
                        product_id = it.productId,
                        quantity = it.quantity,
                        is_refill = it.isRefill,
                    )
                },
                address_id = if (!s.useInlineAddress) s.selectedAddressId else null,
                delivery_address = if (s.useInlineAddress) s.inline.address.trim() else null,
                pincode = if (s.useInlineAddress) s.inline.pincode.trim() else null,
                delivery_name = if (s.useInlineAddress) s.inline.name.trim() else null,
                delivery_phone = if (s.useInlineAddress) s.inline.phone.trim() else null,
                save_address = if (s.useInlineAddress) s.inline.save else null,
                address_label = if (s.useInlineAddress) s.inline.label else null,
                notes = s.notes.trim().ifBlank { null },
                referral_code = s.referralCode.trim().ifBlank { null },
                store_credit_to_apply = est.creditApplied.takeIf { it > 0 },
            )

            apiCall { container.api.placeOrder(request) }
                .onSuccess { res ->
                    pendingOrderId = res.order.id
                    pendingOrderNumber = res.order.order_number
                    pendingDelivery = Triple(
                        res.delivery?.dealerName,
                        res.delivery?.dealerPhone,
                        res.confirmation?.message,
                    )
                    startPayment(res.order.id, res.order.order_number)
                }
                .onFailure { e ->
                    state.value = state.value.copy(stage = CheckoutStage.Form, error = e.message)
                }
        }
    }

    private suspend fun startPayment(orderId: Long, orderNumber: String) {
        apiCall { container.api.createPayment(CreatePaymentRequest(orderId)) }
            .onSuccess { pay ->
                state.value = state.value.copy(stage = CheckoutStage.AwaitingPayment)
                val consumer = state.value.consumer
                openRazorpay.tryEmit(
                    RazorpayPayload(
                        keyId = pay.key_id,
                        razorpayOrderId = pay.razorpay_order_id,
                        amountPaise = pay.amount,
                        currency = pay.currency,
                        orderNumber = orderNumber,
                        name = consumer?.name,
                        email = consumer?.email,
                        phone = consumer?.phone,
                    )
                )
            }
            .onFailure { e ->
                failPayment("Couldn't start the payment: ${e.message}")
            }
    }

    /**
     * Turn Razorpay's failure payload into something a customer can read.
     * The SDK often hands back a raw JSON error blob (or a useless "undefined"),
     * which must never be shown as-is. Prefer a real message parsed out of it;
     * otherwise fall back to a calm, reassuring line.
     */
    private fun humanizePaymentError(raw: String?): String {
        val generic =
            "Payment didn't go through — your cart is untouched. Please try again, " +
                "or use a different payment method."
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return generic

        // A plain sentence from the SDK is safe to surface directly.
        if (!text.startsWith("{")) return "Payment didn't go through: $text"

        // A JSON error blob: pull out a genuine description if there is one.
        val desc = runCatching {
            org.json.JSONObject(text)
                .optJSONObject("error")
                ?.optString("description")
                ?.takeIf { it.isNotBlank() && !it.equals("undefined", true) && !it.equals("null", true) }
        }.getOrNull()
        return if (desc != null) "Payment didn't go through: $desc" else generic
    }

    /** Called when the user backs out of the Razorpay sheet without paying. */
    fun onPaymentDismissed() {
        if (state.value.stage == CheckoutStage.AwaitingPayment) {
            viewModelScope.launch {
                failPayment("Payment cancelled. Your cart is untouched; you can try again.")
            }
        }
    }

    fun clearError() {
        state.value = state.value.copy(error = null)
    }
}
