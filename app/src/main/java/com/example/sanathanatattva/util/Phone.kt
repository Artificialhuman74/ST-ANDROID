package com.example.sanathanatattva.util

/**
 * The 10-digit Indian mobile number inside whatever was typed, pasted or
 * autofilled: "+91 98765 43210", "919876543210" and "098765 43210" all become
 * "9876543210". Only a whole number with its +91 / leading 0 is unwrapped, so a
 * digit typed onto a full number is dropped rather than shifting the number.
 */
fun mobileDigits(raw: String): String {
    val d = raw.filter(Char::isDigit)
    return when {
        d.length == 12 && d.startsWith("91") -> d.drop(2)
        d.length == 11 && d.startsWith("0") -> d.drop(1)
        else -> d.take(10)
    }
}

/** A 10-digit Indian mobile number (starts 6–9). */
fun isMobile(digits: String): Boolean = digits.length == 10 && digits[0] in '6'..'9'
