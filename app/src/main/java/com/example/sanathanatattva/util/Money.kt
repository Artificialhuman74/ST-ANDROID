package com.example.sanathanatattva.util

import java.util.Locale

/**
 * Formats rupee amounts the way the receipts on the website do:
 * whole rupees without decimals ("₹450"), paise only when present ("₹450.50").
 */
fun formatRupees(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite()) return "₹0"
    val sign = if (amount < 0) "-" else ""
    val magnitude = kotlin.math.abs(amount)
    return if (magnitude % 1.0 == 0.0) {
        sign + "₹" + String.format(Locale.ENGLISH, "%,.0f", magnitude)
    } else {
        sign + "₹" + String.format(Locale.ENGLISH, "%,.2f", magnitude)
    }
}
