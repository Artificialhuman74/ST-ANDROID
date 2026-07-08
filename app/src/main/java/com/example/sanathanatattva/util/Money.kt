package com.example.sanathanatattva.util

import java.util.Locale

/**
 * Formats rupee amounts the way the receipts on the website do:
 * whole rupees without decimals ("₹450"), paise only when present ("₹450.50").
 */
fun formatRupees(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        "₹" + String.format(Locale.ENGLISH, "%,.0f", amount)
    } else {
        "₹" + String.format(Locale.ENGLISH, "%,.2f", amount)
    }
}
