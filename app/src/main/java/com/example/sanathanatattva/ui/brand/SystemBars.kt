package com.example.sanathanatattva.ui.brand

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * While [active], system-bar icons render light so they stay legible on the
 * press-green ground of a brand moment; the theme's own choice is restored after.
 */
@Composable
internal fun LightSystemBarIcons(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        val window = (view.context as? Activity)?.window
        val controller = if (active && window != null && !view.isInEditMode) {
            WindowCompat.getInsetsController(window, view)
        } else {
            null
        }
        val lightStatus = controller?.isAppearanceLightStatusBars
        val lightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null && lightStatus != null && lightNav != null) {
                controller.isAppearanceLightStatusBars = lightStatus
                controller.isAppearanceLightNavigationBars = lightNav
            }
        }
    }
}
