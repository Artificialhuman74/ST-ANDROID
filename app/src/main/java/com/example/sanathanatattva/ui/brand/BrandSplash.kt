package com.example.sanathanatattva.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.sanathanatattva.R
import com.example.sanathanatattva.ui.components.EaseOutQuint
import com.example.sanathanatattva.ui.components.GravityEasing
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.theme.LogoCream
import com.example.sanathanatattva.ui.theme.OilGold
import com.example.sanathanatattva.ui.theme.PressGreen
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// App-open splash: "the press turns around the logo".
//
// The system splash is plain press-green (see Theme.SanathanaTattva.Starting),
// so frame 0 here is identical. A cream rim draws; the press beam, yoke at its
// tip, walks once around the logo emblem and unveils it behind it; the tagline
// settles; a drop of oil forms under the emblem and falls; where it lands the
// green opens from the bottom onto the app, edged in oil-gold.
// ---------------------------------------------------------------------------

private const val RIM_START = 0
private const val RIM_MS = 420
private const val PATH_MS = 1450
private const val BEAM_START = 100
private const val BEAM_MS = 1350
private const val TURN_START = 300
private const val TURN_MS = 900
private const val TAGLINE_START = 1350
private const val TAGLINE_MS = 750
private const val DROP_FORM_START = 2000
private const val DROP_FORM_MS = 250
private const val FALL_START = 2250
private const val FALL_MS = 300
private const val RIPPLE_START = 2540
private const val RIPPLE_MS = 450
private const val REVEAL_START = 2530
private const val REVEAL_MS = 500
private const val SPLASH_TOTAL_MS = REVEAL_START + REVEAL_MS

/** The emblem's vertical position, as a fraction of the screen height. */
private const val CENTER_Y_FRACTION = 0.45f

/**
 * Plays the brand splash over whatever is beneath it, then calls [onFinished].
 * Under reduced motion it finishes immediately (the system splash already
 * showed the brand ground).
 */
@Composable
fun BrandSplash(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    if (rememberReducedMotion()) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }
    val clock = rememberMomentClock(SPLASH_TOTAL_MS, onFinished)
    val logo = ImageBitmap.imageResource(R.drawable.logo)
    val drop = rememberOilDropPath()
    LightSystemBarIcons(active = true)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .blockTouches()
            .semantics { contentDescription = "Sanathana Tattva" }
            // Offscreen so the reveal can punch a real hole through to the app.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawBottomReveal(clock.value)
            }
            .background(PressGreen),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            val center = Offset(size.width / 2f, size.height * CENTER_Y_FRACTION)
            drawLogoUnveil(logo, center, 104.dp.toPx(), phase(t, TURN_START, TURN_MS))
            drawCreamRim(center, phase(t, RIM_START, RIM_MS, EaseOutQuint))
            drawBullockPath(center, t)
            drawPressBeam(center, t)
            drawFallingDrop(drop, center, t)
            drawImpactRipple(t)
        }
        Tagline(
            clock = clock,
            modifier = Modifier.offset(y = maxHeight * CENTER_Y_FRACTION + 180.dp),
        )
    }
}

@Composable
private fun Tagline(clock: State<Float>, modifier: Modifier = Modifier) {
    Text(
        "Purity of tradition in every drop",
        style = MaterialTheme.typography.bodyLarge,
        fontStyle = FontStyle.Italic,
        color = LogoCream.copy(alpha = 0.78f),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                val t = clock.value
                // In over the first 60% (rising 10dp), out over the last 20%.
                val enter = phase(t, TAGLINE_START, (TAGLINE_MS * 0.6f).toInt(), EaseOutQuint)
                val leave = phase(t, TAGLINE_START + (TAGLINE_MS * 0.8f).toInt(), (TAGLINE_MS * 0.2f).toInt())
                alpha = enter * (1f - leave)
                translationY = (1f - enter) * 10.dp.toPx()
            },
    )
}

/** The logo emblem, revealed clockwise from 12 o'clock as the beam sweeps past. */
private fun DrawScope.drawLogoUnveil(logo: ImageBitmap, center: Offset, radius: Float, sweep: Float) {
    if (sweep <= 0f) return
    clipPath(Path().apply { addOval(Rect(center, radius)) }) {
        if (sweep >= 1f) {
            drawEmblem(logo, center, radius)
        } else {
            val wedge = Path().apply {
                moveTo(center.x, center.y)
                arcTo(Rect(center, radius * 1.5f), -90f, 360f * sweep, false)
                close()
            }
            clipPath(wedge) { drawEmblem(logo, center, radius) }
        }
    }
}

/** Crops the circular emblem out of the logo artwork and fits it to [radius]. */
private fun DrawScope.drawEmblem(logo: ImageBitmap, center: Offset, radius: Float) {
    // In the 512×279 artwork the emblem's ring is centred at (256, 141), r ≈ 101.
    val srcCx = logo.width * 0.5f
    val srcCy = logo.height * (141f / 279f)
    val srcR = logo.width * (101f / 512f)
    val srcSide = (srcR * 2f).roundToInt()
    val dstSide = (radius * 2f).roundToInt()
    drawImage(
        image = logo,
        srcOffset = IntOffset((srcCx - srcR).roundToInt(), (srcCy - srcR).roundToInt()),
        srcSize = IntSize(srcSide, srcSide),
        dstOffset = IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt()),
        dstSize = IntSize(dstSide, dstSide),
        filterQuality = FilterQuality.High,
    )
}

/** The cream rim around the emblem, drawn clockwise from 12 o'clock. */
private fun DrawScope.drawCreamRim(center: Offset, progress: Float) {
    if (progress <= 0f) return
    val r = 110.dp.toPx()
    drawArc(
        color = LogoCream,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = center - Offset(r, r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = 2.5.dp.toPx()),
    )
}

/** The bullock's circuit: a slowly turning dotted ring outside the emblem. */
private fun DrawScope.drawBullockPath(center: Offset, t: Float) {
    val alpha = lifeAlpha(t, 0, PATH_MS, fadeIn = 0.2f, fadeOut = 0.2f)
    if (alpha <= 0f) return
    rotate(degrees = t / 14_000f * 360f, pivot = center) {
        drawCircle(
            color = LogoCream,
            radius = 150.dp.toPx(),
            center = center,
            alpha = 0.38f * alpha,
            style = Stroke(
                width = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 10.dp.toPx())),
            ),
        )
    }
}

/** The press beam: a spoke from the rim out to the yoke, walking one full turn. */
private fun DrawScope.drawPressBeam(center: Offset, t: Float) {
    val alpha = lifeAlpha(t, BEAM_START, BEAM_MS, fadeIn = 0.15f, fadeOut = 0.18f)
    if (alpha <= 0f) return
    rotate(degrees = 360f * phase(t, TURN_START, TURN_MS), pivot = center) {
        drawLine(
            color = LogoCream,
            start = Offset(center.x, center.y - 112.dp.toPx()),
            end = Offset(center.x, center.y - 146.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha,
        )
        drawCircle(LogoCream, 6.dp.toPx(), Offset(center.x, center.y - 150.dp.toPx()), alpha = alpha)
    }
}

/** A drop gathers under the emblem (growing from its tip), then free-falls to the impact point. */
private fun DrawScope.drawFallingDrop(drop: Path, center: Offset, t: Float) {
    if (t < DROP_FORM_START) return
    val tipY = center.y + 112.dp.toPx()
    val tip = OIL_DROP_TIP_DP.dp.toPx()
    if (t < FALL_START) {
        val s = lerp(0.15f, 1f, phase(t, DROP_FORM_START, DROP_FORM_MS, EaseOutQuint))
        drawOilDrop(drop, Offset(center.x, tipY + tip * s), alpha = s, scaleX = s, scaleY = s)
        return
    }
    val fall = phase(t, FALL_START, FALL_MS, GravityEasing)
    val restY = tipY + tip
    val distance = impactY() - restY
    val fade = 1f - phase(t, FALL_START + FALL_MS - 18, 18)
    drawOilDrop(
        drop,
        Offset(center.x, restY + distance * fall),
        alpha = fade,
        scaleX = lerp(1f, 0.9f, fall),
        scaleY = lerp(1f, 1.16f, fall),
    )
}

/** A flat gold ripple where the drop lands. */
private fun DrawScope.drawImpactRipple(t: Float) {
    val p = phase(t, RIPPLE_START, RIPPLE_MS, EaseOutQuint)
    if (p <= 0f || p >= 1f) return
    val at = Offset(size.width / 2f, impactY())
    val s = lerp(0.4f, 2.6f, p)
    scale(s, s, pivot = at) {
        val rx = 30.dp.toPx()
        val ry = 7.dp.toPx()
        drawOval(
            color = OilGold,
            topLeft = at - Offset(rx, ry),
            size = Size(rx * 2f, ry * 2f),
            alpha = 1f - p,
            style = Stroke(width = 1.5.dp.toPx() / s),
        )
    }
}

/** The green opens from where the drop landed, edged in gold, onto the app beneath. */
private fun DrawScope.drawBottomReveal(t: Float) {
    val p = phase(t, REVEAL_START, REVEAL_MS, EaseOutQuint)
    if (p <= 0f) return
    val origin = Offset(size.width / 2f, impactY())
    val far = hypot(max(origin.x, size.width - origin.x), origin.y) + 24.dp.toPx()
    val r = far * p
    drawCircle(OilGold, radius = r + 12.dp.toPx() * p, center = origin)
    drawCircle(Color.Transparent, radius = r, center = origin, blendMode = BlendMode.Clear)
}

private fun DrawScope.impactY(): Float = size.height - 54.dp.toPx()

/** Fades in over the first [fadeIn] and out over the last [fadeOut] of a window. */
private fun lifeAlpha(t: Float, start: Int, duration: Int, fadeIn: Float, fadeOut: Float): Float {
    val inMs = (duration * fadeIn).toInt()
    val outMs = (duration * fadeOut).toInt()
    return phase(t, start, inMs) * (1f - phase(t, start + duration - outMs, outMs))
}
