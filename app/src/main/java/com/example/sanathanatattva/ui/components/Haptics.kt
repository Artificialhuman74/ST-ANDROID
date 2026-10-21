package com.example.sanathanatattva.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The four feels used at login and logout (design canvas "Haptics"). Each goes through
 * the view, so nothing buzzes when the phone's touch feedback is switched off.
 */
enum class Haptic {
    /** A light tick: a button was pressed and something is starting. */
    Tick,
    /** A firm tap: it worked. */
    Confirm,
    /** A double buzz: wrong password, failed sign-in. */
    Reject,
    /** A soft settle: logged out. */
    Settle,
}

fun View.haptic(kind: Haptic) {
    val sdk = Build.VERSION.SDK_INT
    val constant = when (kind) {
        Haptic.Tick -> HapticFeedbackConstants.CONTEXT_CLICK
        Haptic.Confirm -> if (sdk >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        Haptic.Reject -> if (sdk >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        Haptic.Settle -> when {
            sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.TOGGLE_OFF
            sdk >= Build.VERSION_CODES.R -> HapticFeedbackConstants.GESTURE_END
            else -> HapticFeedbackConstants.VIRTUAL_KEY
        }
    }
    performHapticFeedback(constant)
}

/** `val buzz = rememberHaptics(); buzz(Haptic.Tick)` */
@Composable
fun rememberHaptics(): (Haptic) -> Unit {
    val view = LocalView.current
    return remember(view) { { kind -> view.haptic(kind) } }
}
