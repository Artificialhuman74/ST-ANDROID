package com.example.sanathanatattva.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * App convention: every in-app error / notification appears at the TOP of the
 * screen, never the bottom. This host owns that placement and a short
 * slide-down entrance (honoring reduced motion) so screens don't each
 * re-implement it.
 *
 * Place it as a top-aligned overlay inside a screen's content Box and pass the
 * scaffold's top inset so it sits just below the app bar:
 *
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     // screen content …
 *     TopSnackbarHost(
 *         hostState,
 *         Modifier.align(Alignment.TopCenter)
 *             .padding(top = innerPadding.calculateTopPadding()),
 *     )
 * }
 * ```
 *
 * The [snackbar] slot lets a screen customise the surface (e.g. the gold
 * "deposit saved" money note) while keeping the shared placement and motion.
 */
@Composable
fun TopSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { DefaultTopSnackbar(it) },
) {
    val reducedMotion = rememberReducedMotion()
    val density = LocalDensity.current
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) { data ->
        val startY = if (reducedMotion) 0f else with(density) { (-16).dp.toPx() }
        val offsetY = remember(data) { Animatable(startY) }
        LaunchedEffect(data) { offsetY.animateTo(0f, tween(300, easing = EaseOutQuint)) }
        Box(Modifier.graphicsLayer { translationY = offsetY.value }) {
            snackbar(data)
        }
    }
}

/** The default rounded surface used for top notifications. */
@Composable
fun DefaultTopSnackbar(data: SnackbarData) {
    Snackbar(
        snackbarData = data,
        shape = RoundedCornerShape(12.dp),
    )
}
