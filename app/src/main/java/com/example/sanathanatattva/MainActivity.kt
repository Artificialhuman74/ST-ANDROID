package com.example.sanathanatattva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sanathanatattva.payments.PaymentEvent
import com.example.sanathanatattva.payments.PaymentEvents
import com.example.sanathanatattva.ui.AppRoot
import com.example.sanathanatattva.ui.theme.SanathanaTattvaTheme
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest launches with the press-green starting theme; from here on the
        // app's own theme applies (the brand splash is drawn in Compose).
        setTheme(R.style.Theme_SanathanaTattva)
        super.onCreate(savedInstanceState)
        Checkout.preload(applicationContext)
        enableEdgeToEdge()
        setContent {
            SanathanaTattvaTheme {
                AppRoot()
            }
        }
    }

    override fun onPaymentSuccess(paymentId: String?, data: PaymentData?) {
        PaymentEvents.events.tryEmit(
            PaymentEvent.Success(
                paymentId = paymentId ?: data?.paymentId.orEmpty(),
                orderId = data?.orderId,
                signature = data?.signature,
            )
        )
    }

    override fun onPaymentError(code: Int, description: String?, data: PaymentData?) {
        PaymentEvents.events.tryEmit(PaymentEvent.Failure(description))
    }
}
