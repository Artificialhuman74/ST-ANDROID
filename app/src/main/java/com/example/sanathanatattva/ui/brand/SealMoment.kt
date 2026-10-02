package com.example.sanathanatattva.ui.brand

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

// ---------------------------------------------------------------------------
// "One drop seals it" — the success moment shared by sign-in and order
// placement. The control that just succeeded becomes a drop of oil and falls;
// green floods out from where it lands, a ring of drops seals a short message,
// then the green collapses into where that success now lives.
//
// Screens describe a moment (SealMoment) and hand it to the app-wide
// CelebrationController; CelebrationHost, above the NavHost, plays it — so the
// moment survives the navigation it covers.
// ---------------------------------------------------------------------------

/** Where the drop starts: the control that just succeeded, in root coordinates. */
sealed interface SealOrigin {
    val bounds: Rect

    /** A filled pill button ("Log in"): it narrows to a circle, then becomes the drop. */
    data class Pill(override val bounds: Rect, val fill: Color) : SealOrigin

    /** A circular spinner ("Confirming your payment…"): its arc closes into a ring, then fills. */
    data class Spinner(override val bounds: Rect, val tint: Color) : SealOrigin
}

/** What the ring of drops seals. */
sealed interface SealEmblem {
    data class Initial(val letter: String) : SealEmblem
    data object Check : SealEmblem
}

/** Where the green goes once the message has been seen. */
sealed interface SealLanding {
    /** Shrink into a bottom-dock tab (looked up by the host), then ping it once. */
    data class Tab(val route: String) : SealLanding

    /**
     * Settle as a medallion of [radius] at [center] (root coordinates). The screen
     * draws the identical [OrderSealMedallion] there, so the hand-off is invisible.
     */
    data class Medallion(val center: Offset, val radius: Dp) : SealLanding
}

class SealMoment(
    val origin: SealOrigin,
    val emblem: SealEmblem,
    /** Small line above the headline ("Welcome back,"). */
    val eyebrow: String?,
    /** The big line, set in the emblem serif ("Meera", "Order placed"). */
    val headline: String,
    /** Small line under the headline ("Order 1042"). */
    val caption: String?,
    /** Asked when the collapse begins, so the screen underneath has laid out. Null → fold into the emblem. */
    val landing: () -> SealLanding?,
    /** Called once the green covers the screen: swap or pop the screen underneath here. */
    val onCovered: () -> Unit,
)

/** "Welcome back, Meera" — the sign-in seal, folding into the Account tab. */
fun signInMoment(
    origin: SealOrigin,
    consumerName: String?,
    accountRoute: String,
    onCovered: () -> Unit,
): SealMoment {
    val firstName = consumerName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotBlank() }
    return SealMoment(
        origin = origin,
        emblem = firstName?.let { SealEmblem.Initial(it.take(1).uppercase()) } ?: SealEmblem.Check,
        eyebrow = if (firstName != null) "Welcome back," else null,
        headline = firstName ?: "Welcome back",
        caption = null,
        landing = { SealLanding.Tab(accountRoute) },
        onCovered = onCovered,
    )
}

/** "Order placed" — the order seal, settling as the success page's medallion. */
fun orderPlacedMoment(
    origin: SealOrigin,
    orderNumber: String,
    medallion: () -> SealLanding.Medallion?,
    onCovered: () -> Unit,
): SealMoment = SealMoment(
    origin = origin,
    emblem = SealEmblem.Check,
    eyebrow = null,
    headline = "Order placed",
    caption = orderNumber.takeIf { it.isNotBlank() }?.let { "Order $it" },
    landing = medallion,
    onCovered = onCovered,
)

/** App-wide: screens [play] a moment; [CelebrationHost] renders it above everything. */
@Stable
class CelebrationController {
    var current: SealMoment? by mutableStateOf(null)
        private set

    fun play(moment: SealMoment) {
        current = moment
    }

    internal fun finish(moment: SealMoment) {
        if (current === moment) current = null
    }
}

@Composable
fun rememberCelebrationController(): CelebrationController = remember { CelebrationController() }

/**
 * Renders the controller's current moment. [tabBounds] maps a dock route to its
 * tab's bounds in root coordinates, or null when the dock isn't showing.
 */
@Composable
fun CelebrationHost(
    controller: CelebrationController,
    tabBounds: (route: String) -> Rect?,
    modifier: Modifier = Modifier,
) {
    val moment = controller.current ?: return
    key(moment) {
        SealOverlay(
            moment = moment,
            tabBounds = tabBounds,
            onFinished = { controller.finish(moment) },
            modifier = modifier,
        )
    }
}
