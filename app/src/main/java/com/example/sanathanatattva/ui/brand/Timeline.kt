package com.example.sanathanatattva.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first

// Brand moments are authored as fixed millisecond timelines (the motion spec on
// the design canvas). One linear clock drives a whole moment; every element maps
// that clock onto its own window with [phase]. Draw code reads the clock inside
// draw/graphicsLayer lambdas, so a running moment never recomposes.

/** 0 before [start], 1 after [start] + [duration], eased in between. */
internal fun phase(t: Float, start: Int, duration: Int, easing: Easing = LinearEasing): Float {
    if (duration <= 0) return if (t >= start) 1f else 0f
    return easing.transform(((t - start) / duration).coerceIn(0f, 1f))
}

/**
 * A millisecond clock that runs once from 0 to [totalMs], then calls [onEnd].
 * It is a plain tween, so it honours the system animator-duration scale.
 */
@Composable
internal fun rememberMomentClock(totalMs: Int, onEnd: () -> Unit): State<Float> {
    val clock = remember { Animatable(0f) }
    val end by rememberUpdatedState(onEnd)
    LaunchedEffect(Unit) {
        clock.animateTo(totalMs.toFloat(), tween(totalMs, easing = LinearEasing))
        end()
    }
    return clock.asState()
}

/** Runs [action] once, the first time [clock] reaches [atMs]. */
@Composable
internal fun OnCue(clock: State<Float>, atMs: Int, action: () -> Unit) {
    val current by rememberUpdatedState(action)
    LaunchedEffect(clock, atMs) {
        snapshotFlow { clock.value >= atMs }.first { it }
        current()
    }
}
