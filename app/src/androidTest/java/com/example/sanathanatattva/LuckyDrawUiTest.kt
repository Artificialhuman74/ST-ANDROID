package com.example.sanathanatattva

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sanathanatattva.data.model.*
import com.example.sanathanatattva.ui.screens.luckydraw.*
import com.example.sanathanatattva.ui.theme.SanathanaTattvaTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** These render fixture data only: no API requests, enrollment or real payments. */
@RunWith(AndroidJUnit4::class)
class LuckyDrawUiTest {
    @get:Rule val compose = createComposeRule()
    private val enrollment = DrawEnrollment(1, 2, "Round One", "running", "won")
    private val winner = DrawCycle(1, 2, "Round One", month_number = 12, won = true,
        reward = DrawReward(product_name = "Groundnut Oil 2.8L", months_remaining = 0))

    @Test fun finalMonthOffersFreeClaim() {
        var claims = 0
        compose.setContent { SanathanaTattvaTheme {
            WinnerCard(winner, winner.reward!!, claiming = false, enabled = true, justClaimed = false, error = null,
                onClaim = { claims++ }, onHowItWorks = null)
        } }
        compose.onNodeWithText("Place my free order · ₹0").assertIsEnabled().performClick()
        assertEquals(1, claims)
        compose.onNodeWithText("Month 12 of 12").assertIsDisplayed()
    }

    @Test fun deliveredRewardDoesNotOfferAnotherClaim() {
        compose.setContent { SanathanaTattvaTheme {
            WinnerCard(winner, winner.reward!!.copy(this_month_ordered = true, this_month_delivered = true),
                claiming = false, enabled = true, justClaimed = false, error = null, onClaim = {}, onHowItWorks = null)
        } }
        compose.onNodeWithText("Delivered. That was the last one of your round.").assertIsDisplayed()
        compose.onNodeWithText("Place my free order · ₹0").assertDoesNotExist()
    }

    @Test fun failedClaimOffersRetryOnTheCard() {
        var claims = 0
        compose.setContent { SanathanaTattvaTheme {
            WinnerCard(winner, winner.reward!!, claiming = false, enabled = true, justClaimed = false,
                error = "Temporarily unavailable", onClaim = { claims++ }, onHowItWorks = null)
        } }
        compose.onNodeWithText("Temporarily unavailable").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        assertEquals(1, claims)
    }

    @Test fun waitingResultNamesNobodyAndOpensTheReveal() {
        var opened = false
        compose.setContent { SanathanaTattvaTheme {
            ResultWaitingCard(winner.copy(latest_draw = DrawResult("2026-10", winner_display_name = "Meera K."))) { opened = true }
        } }
        compose.onNodeWithText("Round One has been drawn.").assertIsDisplayed()
        compose.onNodeWithText("Meera K.", substring = true).assertDoesNotExist()
        compose.onNodeWithText("See the result").performClick()
        assertTrue(opened)
    }

    @Test fun revealHoldsItsButtonUntilContinue() {
        var continued = false
        compose.mainClock.autoAdvance = false
        compose.setContent { SanathanaTattvaTheme {
            WinMoment("Meera K.", isMe = false, hasWinner = true, schemeName = "Round One", rewardProductName = null,
                nextEntryDeadline = "2026-11-12T11:30:00Z", hasNextDraw = true,
                onContinue = { continued = true }, onBack = {})
        } }
        compose.mainClock.advanceTimeBy(14_000)
        compose.onNodeWithText("Shop now to enter").assertIsEnabled().performClick()
        assertTrue(continued)
    }

    @Test fun missingPrerequisitesDisablePaidEnrollment() {
        compose.setContent { SanathanaTattvaTheme {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                EnrollmentForm(LuckyDrawUiState(loading = false,
                    status = LuckyDrawStatus(open_schemes = listOf(DrawScheme(2, "Round One", 100.0))),
                    consumer = Consumer(7), addresses = emptyList()), { _, _ -> error("Must not enroll") }, {}, {})
            }
        } }
        compose.onNodeWithText("Join Round One · ₹100").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Link your trader").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Add address").performScrollTo().assertIsEnabled()
    }
}
