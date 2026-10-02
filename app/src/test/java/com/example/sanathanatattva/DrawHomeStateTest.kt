package com.example.sanathanatattva

import com.example.sanathanatattva.data.model.*
import com.example.sanathanatattva.ui.screens.luckydraw.*
import org.junit.Assert.*
import org.junit.Test

class DrawHomeStateTest {
    private val scheme = DrawScheme(2, "Round One", 100.0)
    private val enrollment = DrawEnrollment(1, 2, "Round One", "running", "active", "2026-10-01 00:00:00")
    private val cycle = DrawCycle(1, 2, "Round One", month_number = 4, month_key = "2026-10",
        entry_deadline = "2026-10-12T11:30:00Z", draw_at = "2026-10-12T11:30:00Z",
        next_draw_at = "2026-11-12T11:30:00Z", next_entry_deadline = "2026-11-12T11:30:00Z")
    private val draw = DrawResult("2026-10", ran_at = "2026-10-12T11:30:00Z", winner_display_name = "Meera K.",
        reward_product_name = "Groundnut Oil 1L")
    private val beforeDeadline = drawTimestamp("2026-10-05T00:00:00Z")!!
    private val afterDeadline = drawTimestamp("2026-10-20T00:00:00Z")!!

    private fun card(status: LuckyDrawStatus?, unseen: Boolean = false, dismissed: Boolean = false) =
        homeCardFor(status, unseen, dismissed)

    @Test fun `anyone not in a round sees the promotion with their next step`() {
        val loading = card(null) as DrawHomeCard.Promo
        assertEquals(PromoCopy("Every month you order, you're in that month's draw.", "How it works"), promoCopy(loading))
        val closed = card(LuckyDrawStatus()) as DrawHomeCard.Promo
        assertTrue(closed.noRoundOpen)
        assertEquals("Learn more", promoCopy(closed).action)
        val open = card(LuckyDrawStatus(open_schemes = listOf(scheme))) as DrawHomeCard.Promo
        assertEquals(scheme, open.scheme)
        assertEquals("Join for ₹100", promoCopy(open).action)
        val unpaid = card(LuckyDrawStatus(active_enrollment = enrollment.copy(status = "pending_payment"),
            open_schemes = listOf(scheme))) as DrawHomeCard.Promo
        assertEquals("Finish joining", promoCopy(unpaid).action)
        assertEquals("Log in to join", promoCopy(DrawHomeCard.Promo(signedIn = false)).action)
    }

    @Test fun `enrollment replaces the promotion with the row`() {
        assertEquals(DrawHomeCard.Joined("Round One", false), card(LuckyDrawStatus(active_enrollment = enrollment)))
        assertEquals(DrawHomeCard.Enrolled(cycle), card(LuckyDrawStatus(active_enrollment = enrollment, cycle = cycle)))
    }

    @Test fun `an unwatched result pre-empts everything, the winner card included`() {
        val won = cycle.copy(won = true, latest_draw = draw.copy(is_me = true), reward = DrawReward(product_name = "Oil"))
        val status = LuckyDrawStatus(active_enrollment = enrollment.copy(status = "won"), cycle = won)
        assertTrue(card(status, unseen = true) is DrawHomeCard.ResultWaiting)
        assertTrue(card(status) is DrawHomeCard.Winner)
    }

    @Test fun `someone else's win shows once watched, until dismissed`() {
        val status = LuckyDrawStatus(active_enrollment = enrollment, cycle = cycle.copy(latest_draw = draw))
        assertEquals(DrawHomeCard.Result(status.cycle!!, draw), card(status))
        assertEquals(DrawHomeCard.Enrolled(status.cycle!!), card(status, dismissed = true))
        // Last month's draw is not news, and a draw held before joining was never theirs.
        val stale = status.copy(cycle = cycle.copy(latest_draw = draw.copy(month_key = "2026-09")))
        assertTrue(card(stale) is DrawHomeCard.Enrolled)
        val joinedLater = status.copy(active_enrollment = enrollment.copy(enrolled_at = "2026-10-20 00:00:00"))
        assertTrue(card(joinedLater) is DrawHomeCard.Enrolled)
    }

    @Test fun `enrolled line follows the month through entry and the draw`() {
        assertEquals(EnrolledLine("Buy any oil by 12 October to enter this month's draw", false),
            enrolledLine(cycle, beforeDeadline))
        assertEquals("Buy any oil to enter the 12 November draw", enrolledLine(cycle, afterDeadline).text)
        assertEquals(EnrolledLine("In this month's draw · 12 October", true), enrolledLine(cycle.copy(entered = true)))
        assertEquals("In this month's draw with 3 entries · 12 October",
            enrolledLine(cycle.copy(entered = true, tickets = 3)).text)
        val drawn = cycle.copy(entered = true, latest_draw = draw)
        assertEquals(EnrolledLine("This month's draw has been held · buy any oil to enter the 12 November draw", false),
            enrolledLine(drawn))
        // Month twelve has no next draw to promise.
        assertEquals("Your round's final draw has been held",
            enrolledLine(drawn.copy(month_number = 12, total_months = 12)).text)
    }

    @Test fun `result copy names the winner or the empty spoke and the next deadline`() {
        val c = cycle.copy(latest_draw = draw)
        assertEquals("Meera K. won Round One.", resultHeadline(c, draw))
        assertEquals("They're getting Groundnut Oil 1L free for the rest of the round. The next draw is on " +
            "12 November. Any order by 12 November puts you in it.", resultBody(c, draw))
        val empty = draw.copy(has_winner = false, winner_display_name = null)
        assertEquals("No one won Round One this month.", resultHeadline(c, empty))
        assertTrue(resultBody(c, empty).startsWith("Nobody ordered in time"))
        assertTrue(resultBody(c.copy(month_number = 12), draw).endsWith("That was your round's final draw."))
    }

    @Test fun `winner card tracks the month from ready to delivered`() {
        val reward = DrawReward(product_name = "Oil", won_at_month = 4, months_remaining = 8)
        assertEquals("October, ready", winnerLabel(cycle, reward, claimed = false))
        assertEquals("October, booked", winnerLabel(cycle, reward, claimed = true))
        assertEquals("October, delivered", winnerLabel(cycle, reward.copy(this_month_delivered = true), true))
        assertEquals("October, the last of your round", winnerLabel(cycle, reward.copy(months_remaining = 0), false))
        assertEquals("Your dealer is packing it.", winnerConfirmation(cycle, reward.copy(this_month_ordered = true)))
        assertEquals("Out for delivery today.",
            winnerConfirmation(cycle, reward.copy(this_month_delivery_status = "out_for_delivery")))
        assertEquals("Delivered. That was the last one of your round.",
            winnerConfirmation(cycle, reward.copy(this_month_delivered = true, months_remaining = 0)))
        assertEquals("Your round is complete — 9 free deliveries after you won in month 4.",
            roundCompleteLine(cycle.copy(total_months = 12), reward))
        assertEquals("October", drawMonthName("2026-10"))
        assertEquals("This month", drawMonthName("bad"))
    }
}
