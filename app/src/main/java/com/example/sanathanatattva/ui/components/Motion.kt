package com.example.sanathanatattva.ui.components

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * The app's motion voice: a confident deceleration (ease-out-quint). Entrances
 * and reveals decelerate into rest; nothing bounces or overshoots.
 */
val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Free fall: starts slow and keeps accelerating. Reserved for things that
 * actually drop (the brand moments' oil drop) — never for UI chrome.
 */
val GravityEasing = CubicBezierEasing(0.55f, 0f, 1f, 0.45f)

/** Exits accelerate away (M3 emphasized-accelerate), the mirror of [EaseOutQuint]. */
val ExitAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/**
 * True when the user has turned animations off at the system level
 * (animator duration scale = 0). Animated surfaces must render their
 * fully-composed rest state instead.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
