package com.example.sanathanatattva.payments

import kotlinx.coroutines.flow.MutableSharedFlow

sealed interface PaymentEvent {
    data class Success(val paymentId: String, val orderId: String?, val signature: String?) : PaymentEvent
    data class Failure(val description: String?) : PaymentEvent
}

/**
 * Razorpay delivers results to the Activity; checkout ViewModels listen here.
 * extraBufferCapacity keeps emit() from suspending on the callback thread.
 */
object PaymentEvents {
    val events = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 4)
}
