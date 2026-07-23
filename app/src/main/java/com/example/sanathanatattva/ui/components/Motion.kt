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
