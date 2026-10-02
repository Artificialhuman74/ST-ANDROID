package com.example.sanathanatattva.ui.screens.luckydraw

import com.example.sanathanatattva.data.model.DrawCycle
import com.example.sanathanatattva.data.model.DrawEnrollment
import com.example.sanathanatattva.data.model.DrawResult
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val LUCKY_DRAW_NAME = "Bhagya Chakra"

/** Database timestamps without an offset are UTC, never the phone's local timezone. */
fun drawTimestamp(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss")) {
        val position = ParsePosition(0)
        val date = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(value, position)
        if (date != null && position.index == value.length) return date.time
    }
    return null
}

fun drawDate(value: String?): String = drawTimestamp(value)?.let {
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.forLanguageTag("en-IN")).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }.format(Date(it)) + " IST"
} ?: "To be announced"

fun belongsToEnrollment(result: DrawResult, enrollment: DrawEnrollment?): Boolean {
    val ranAt = drawTimestamp(result.ran_at) ?: return false
    val joinedAt = drawTimestamp(enrollment?.enrolled_at) ?: return false
    return ranAt >= joinedAt
}

fun checkoutDrawMessage(cycle: DrawCycle?, now: Long = System.currentTimeMillis()): String? {
    if (cycle == null || cycle.won || cycle.round_over) return null
    val deadline = drawTimestamp(cycle.entry_deadline) ?: return null
    val rollsForward = now > deadline
    if (rollsForward && cycle.month_number >= cycle.total_months) return null
    val date = if (rollsForward) cycle.next_draw_at else cycle.draw_at
    return "A qualifying paid purchase enters you in ${cycle.scheme_name}'s " +
        "${if (rollsForward) "next" else "current"} draw on ${drawDate(date)}. " +
        "Win and receive your reward oil free each month for the rest of the round."
}

fun luckyDrawOrderLabel(number: String): String? = when {
    number.startsWith("BCGIFT-") -> "$LUCKY_DRAW_NAME welcome gift"
    number.startsWith("BCFREE-") -> "$LUCKY_DRAW_NAME reward"
    number.startsWith("BCGUAR-") -> "$LUCKY_DRAW_NAME guarantee"
    else -> null
}
