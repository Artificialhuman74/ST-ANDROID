package com.example.sanathanatattva.ui.screens.luckydraw

import com.example.sanathanatattva.data.model.DrawCycle
import com.example.sanathanatattva.data.model.DrawResult
import com.example.sanathanatattva.data.model.DrawReward
import com.example.sanathanatattva.data.model.DrawScheme
import com.example.sanathanatattva.data.model.LuckyDrawStatus
import com.example.sanathanatattva.util.formatRupees
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The one Bhagya Chakra card the home page shows, mirroring the web shop page.
 * Checked in this order:
 *
 *  1. A draw has run and this account hasn't watched the reveal: the reveal is where
 *     finding out happens, so nothing else (the winner's claim card included) pre-empts it.
 *  2. Won: the winner's card, every month until the round ends.
 *  3. Watched, someone else won this month: who won, and how to be in the next draw.
 *  4. Enrolled: the quiet one-line row (the default all month).
 *  5. Everyone else: the promotional banner, whose call to action follows what they can
 *     do next (join an open round, finish a payment, or learn how it works).
 */
sealed interface DrawHomeCard {
    data class ResultWaiting(val cycle: DrawCycle) : DrawHomeCard
    data class Winner(val cycle: DrawCycle, val reward: DrawReward) : DrawHomeCard
    data class Result(val cycle: DrawCycle, val draw: DrawResult) : DrawHomeCard
    data class Enrolled(val cycle: DrawCycle) : DrawHomeCard
    /** Joined, but the round isn't running yet, so there's no month to report on. */
    data class Joined(val schemeName: String, val won: Boolean) : DrawHomeCard
    /**
     * The banner for anyone not yet in a round. [scheme] is the round open to join, if any;
     * [noRoundOpen] is only true once the server has said so (not while still loading).
     */
    data class Promo(
        val signedIn: Boolean,
        val scheme: DrawScheme? = null,
        val noRoundOpen: Boolean = false,
        val unfinishedPayment: Boolean = false,
    ) : DrawHomeCard
}

fun homeCardFor(status: LuckyDrawStatus?, unseenResult: Boolean, resultDismissed: Boolean): DrawHomeCard {
    if (status == null) return DrawHomeCard.Promo(signedIn = true)
    val cycle = status.cycle
    val draw = cycle?.latest_draw
    if (cycle != null && draw != null && unseenResult) return DrawHomeCard.ResultWaiting(cycle)
    val reward = cycle?.reward
    if (cycle != null && cycle.won && reward != null) return DrawHomeCard.Winner(cycle, reward)
    // Only this month's draw is news, and only one they were in.
    val fresh = draw?.takeIf { cycle?.month_key == it.month_key && belongsToEnrollment(it, status.enrollment) }
    if (cycle != null && fresh != null && !fresh.is_me && !resultDismissed) return DrawHomeCard.Result(cycle, fresh)
    status.enrollment?.let { enrollment ->
        return if (cycle != null) DrawHomeCard.Enrolled(cycle)
        else DrawHomeCard.Joined(enrollment.scheme_name, enrollment.status == "won")
    }
    val open = status.open_schemes.firstOrNull()
    return DrawHomeCard.Promo(
        signedIn = true,
        scheme = open,
        noRoundOpen = open == null,
        unfinishedPayment = status.active_enrollment?.status == "pending_payment",
    )
}

/** What the banner says under its headline, and on its button. */
data class PromoCopy(val body: String, val action: String)

fun promoCopy(promo: DrawHomeCard.Promo): PromoCopy {
    val entry = "Every month you order, you're in that month's draw."
    return when {
        !promo.signedIn -> PromoCopy("$entry Log in to see this month's round.", "Log in to join")
        promo.unfinishedPayment -> PromoCopy("Your joining payment didn't go through. Pick up where you left off.", "Finish joining")
        promo.scheme != null -> PromoCopy("$entry Pick a free 200ml oil when you join.", "Join for ${formatRupees(promo.scheme.joining_fee)}")
        promo.noRoundOpen -> PromoCopy("The next round opens soon. Every month you order, you'll be in that month's draw.", "Learn more")
        else -> PromoCopy(entry, "How it works")
    }
}

private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

/** "12 October", in IST: the API's draw times are fixed IST wall-clock times. */
fun drawDayMonth(value: String?): String? = drawTimestamp(value)?.let {
    SimpleDateFormat("d MMMM", Locale.forLanguageTag("en-IN")).apply { timeZone = IST }.format(Date(it))
}

/** "October" from a `YYYY-MM` month key. */
fun drawMonthName(monthKey: String): String {
    val month = monthKey.substringAfter('-', "").toIntOrNull()
    return if (month != null && month in 1..12) DateFormatSymbols(Locale.ENGLISH).months[month - 1] else "This month"
}

private fun DrawCycle.hasNextDraw() = !round_over && month_number < total_months

data class EnrolledLine(val text: String, val inThisDraw: Boolean)

/**
 * What the enrolled row says. "You're in X" is true all year and tells nobody anything,
 * so the line carries whether they're in THIS month's draw, and when they aren't, the
 * one thing that would fix it. Once this month's draw has run, `entered` stays true, so
 * the row must stop promising a draw that has already been settled.
 */
fun enrolledLine(cycle: DrawCycle, now: Long = System.currentTimeMillis()): EnrolledLine {
    val drawn = cycle.latest_draw?.month_key == cycle.month_key
    val inThisDraw = cycle.entered && !drawn && !cycle.round_over
    val nextLine = drawDayMonth(cycle.next_draw_at)?.takeIf { cycle.hasNextDraw() }
        ?.let { "Buy any oil to enter the $it draw" }
    val deadline = drawTimestamp(cycle.entry_deadline)
    val windowOpen = deadline != null && now <= deadline
    val drawDay = drawDayMonth(cycle.draw_at)
    val text = when {
        cycle.round_over -> "Your round has finished"
        drawn && nextLine == null -> "Your round's final draw has been held"
        drawn && cycle.entered -> "This month's draw has been held · ${nextLine!!.replaceFirstChar { it.lowercase() }}"
        drawn -> nextLine!!
        inThisDraw -> buildString {
            append("In this month's draw")
            if (cycle.tickets > 1) append(" with ${cycle.tickets} entries")
            drawDay?.let { append(" · $it") }
        }
        windowOpen -> "Buy any oil by ${drawDayMonth(cycle.entry_deadline)} to enter this month's draw"
        nextLine != null -> nextLine
        else -> "Your round's final draw is coming up"
    }
    return EnrolledLine(text, inThisDraw)
}

fun resultHeadline(cycle: DrawCycle, draw: DrawResult): String =
    if (!draw.has_winner) "No one won ${cycle.scheme_name} this month."
    else "${draw.winner_display_name ?: "A member of your round"} won ${cycle.scheme_name}."

fun resultBody(cycle: DrawCycle, draw: DrawResult): String = buildString {
    when {
        !draw.has_winner -> append("Nobody ordered in time, so the wheel stopped on an empty spoke. ")
        draw.reward_product_name != null -> append("They're getting ${draw.reward_product_name} free for the rest of the round. ")
    }
    val next = drawDayMonth(cycle.next_draw_at)?.takeIf { cycle.hasNextDraw() }
    if (next == null) {
        append("That was your round's final draw.")
    } else {
        append("The next draw is on $next.")
        drawDayMonth(cycle.next_entry_deadline)?.let { append(" Any order by $it puts you in it.") }
    }
}

/** The small label over the winner's product: where this month's delivery stands. */
fun winnerLabel(cycle: DrawCycle, reward: DrawReward, claimed: Boolean): String {
    val month = drawMonthName(cycle.month_key)
    return when {
        reward.this_month_delivered -> "$month, delivered"
        reward.months_remaining <= 0 -> "$month, the last of your round"
        claimed -> "$month, booked"
        else -> "$month, ready"
    }
}

/** Where the delivery has got to, in the words someone waiting for a can of oil would use. */
fun winnerConfirmation(cycle: DrawCycle, reward: DrawReward): String {
    val lastOne = reward.months_remaining <= 0
    if (reward.this_month_delivered) return if (lastOne) "Delivered. That was the last one of your round." else "Delivered."
    return when (reward.this_month_delivery_status ?: "pending") {
        "pending", "accepted" -> "Your dealer is packing it."
        "packed" -> "Packed and waiting to go out."
        "out_for_delivery" -> "Out for delivery today."
        else -> when {
            lastOne -> "That's the last delivery of your round."
            reward.won_at_month == cycle.month_number -> "Your first free delivery is on its way."
            else -> "On its way to you."
        }
    }
}

/** Counted from the month they actually won, not a boast about twelve deliveries. */
fun roundCompleteLine(cycle: DrawCycle, reward: DrawReward): String {
    val count = cycle.total_months - reward.won_at_month + 1
    return if (reward.won_at_month in 1..cycle.total_months) {
        "Your round is complete — $count free ${if (count == 1) "delivery" else "deliveries"} after you won in month ${reward.won_at_month}."
    } else "Your round is complete."
}
