package com.example.sanathanatattva

import com.example.sanathanatattva.data.model.*
import com.example.sanathanatattva.ui.screens.luckydraw.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class LuckyDrawRulesTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }
    private val cycle = DrawCycle(1, 2, "Round One", entry_deadline = "2026-10-12T11:30:00.000Z",
        draw_at = "2026-10-12T11:30:00.000Z", next_draw_at = "2026-11-12T11:30:00.000Z")

    @Test fun `database dates are UTC and labels always use IST`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(drawTimestamp("2026-10-12T11:30:00Z"), drawTimestamp("2026-10-12 11:30:00"))
            assertEquals(drawTimestamp("2026-10-12T11:30:00Z"), drawTimestamp("2026-10-12T17:00:00+05:30"))
            assertTrue(drawDate("2026-10-12T11:30:00.000Z").equals("12 Oct 2026, 5:00 PM IST", ignoreCase = true))
            assertNull(drawTimestamp("not-a-date"))
            assertNull(drawTimestamp("2026-02-31 11:30:00"))
        } finally { TimeZone.setDefault(original) }
    }

    @Test fun `late purchases name next draw and final round cannot promise a thirteenth draw`() {
        val deadline = drawTimestamp(cycle.entry_deadline)!!
        assertTrue(checkoutDrawMessage(cycle, deadline)!!.contains("current draw on 12 Oct"))
        assertTrue(checkoutDrawMessage(cycle, deadline + 1)!!.contains("next draw on 12 Nov"))
        assertNull(checkoutDrawMessage(cycle.copy(month_number = 12), deadline + 1))
        assertNull(checkoutDrawMessage(cycle.copy(won = true), deadline))
        assertNull(checkoutDrawMessage(cycle.copy(round_over = true), deadline))
    }

    @Test fun `winner can claim month twelve with zero subsequent months remaining`() {
        val finalMonth = cycle.copy(won = true, month_number = 12, reward = DrawReward(months_remaining = 0))
        assertTrue(finalMonth.canClaim)
        assertFalse(finalMonth.copy(round_over = true).canClaim)
        assertFalse(finalMonth.copy(reward = DrawReward(this_month_ordered = true)).canClaim)
        assertFalse(cycle.canClaim)
    }

    @Test fun `draws held before joining do not trigger a reveal`() {
        val result = DrawResult("2026-10", ran_at = "2026-10-12T11:30:00.000Z")
        val enrollment = DrawEnrollment(1, 2, status = "active", enrolled_at = "2026-10-20 10:00:00")
        assertFalse(belongsToEnrollment(result, enrollment))
        assertTrue(belongsToEnrollment(result, enrollment.copy(enrolled_at = "2026-10-01 10:00:00")))
        assertFalse(belongsToEnrollment(result.copy(ran_at = null), enrollment))
    }

    @Test fun `status decodes pending payment nullable winner and additional backend fields`() {
        val status = json.decodeFromString<LuckyDrawStatus>("""{
          "active_enrollment":{"id":1,"scheme_id":2,"status":"pending_payment","welcome_gift_product_id":9},
          "open_schemes":[{"id":2,"name":"Round One","joining_fee":100,"enrollment_closes_at":null}],
          "draw_schedule":{"day":15,"time":"16:30"},
          "cycle":{"enrollment_id":1,"scheme_id":2,"scheme_name":"Round One",
            "latest_draw":{"month_key":"2026-10","has_winner":false,"winner_display_name":null,"is_me":false},
            "reward":null,"entry_order_number":null,"draw_day":15}
        }""")
        assertNull(status.enrollment)
        assertEquals(1, status.open_schemes.size)
        assertEquals(100.0, status.open_schemes.first().joining_fee, 0.0)
        assertEquals(15, status.draw_schedule.day)
        assertFalse(status.cycle!!.latest_draw!!.has_winner)
    }

    @Test fun `declining gift sends an explicit false and verification uses enrollment id`() {
        assertEquals("""{"scheme_id":2,"welcome_gift":false}""", json.encodeToString(EnrollDrawRequest(2, false)))
        val body = json.encodeToString(VerifyDrawRequest(1, "order_test", "pay_test", "sig_test"))
        assertTrue(body.contains("\"enrollment_id\":1"))
        assertFalse(body.contains("consumer_order_id"))
        assertTrue(json.decodeFromString<DrawActionResponse>("""{"alreadyProcessed":true,"enrollment":{"id":1}}""").alreadyProcessed)
    }

    @Test fun `all free order kinds and reward containers are recognized`() {
        assertEquals("Bhagya Chakra welcome gift", luckyDrawOrderLabel("BCGIFT-123"))
        assertEquals("Bhagya Chakra reward", luckyDrawOrderLabel("BCFREE-123"))
        assertEquals("Bhagya Chakra guarantee", luckyDrawOrderLabel("BCGUAR-123"))
        assertNull(luckyDrawOrderLabel("ORD-123"))
        assertEquals("lucky_draw_reward", json.decodeFromString<ContainerHolding>("""{"id":1,"source":"lucky_draw_reward"}""").source)
    }
}
