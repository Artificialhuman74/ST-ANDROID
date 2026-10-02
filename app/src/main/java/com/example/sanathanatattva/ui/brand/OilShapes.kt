package com.example.sanathanatattva.ui.brand

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.sanathanatattva.ui.theme.OilDeep
import com.example.sanathanatattva.ui.theme.OilLight
import kotlin.math.hypot

// The few shapes every brand moment shares: the oil drop (from the logo), the
// small drops that ring the emblem, and the seal's check. Geometry is in dp,
// centred on the shape's own origin, so callers only translate/scale it.

/** Height of the big oil drop from tip to base, in dp (tip at -24, base at +21.6). */
internal const val OIL_DROP_TIP_DP = 24f

/** The logo's oil drop, centred on its own origin (tip at y = -24dp). */
@Composable
internal fun rememberOilDropPath(): Path {
    val density = LocalDensity.current
    return remember(density) {
        val u = with(density) { 1.dp.toPx() }
        Path().apply {
            moveTo(0f, -24f * u)
            cubicTo(4.8f * u, -15.2f * u, 15.2f * u, -6.4f * u, 15.2f * u, 6.4f * u)
            arcTo(Rect(Offset(0f, 6.4f * u), 15.2f * u), 0f, 180f, false)
            cubicTo(-15.2f * u, -6.4f * u, -4.8f * u, -15.2f * u, 0f, -24f * u)
            close()
        }
    }
}

/** One of the small drops that ring the emblem, centred on its own origin, tip up. */
@Composable
internal fun rememberRingDropPath(): Path {
    val density = LocalDensity.current
    return remember(density) {
        val u = with(density) { 1.dp.toPx() }
        Path().apply {
            moveTo(0f, -7f * u)
            cubicTo(1.8f * u, -4f * u, 4f * u, -1.8f * u, 4f * u, 1.2f * u)
            arcTo(Rect(Offset(0f, 1.2f * u), 4f * u), 0f, 180f, false)
            cubicTo(-4f * u, -1.8f * u, -1.8f * u, -4f * u, 0f, -7f * u)
            close()
        }
    }
}

/**
 * Draws the oil drop with [path]'s origin at [at]: an amber gradient body and a
 * soft highlight, stretched by [scaleX]/[scaleY] about its centre.
 */
internal fun DrawScope.drawOilDrop(
    path: Path,
    at: Offset,
    alpha: Float = 1f,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
) {
    if (alpha <= 0f) return
    val u = 1.dp.toPx()
    withTransform({
        translate(at.x, at.y)
        scale(scaleX, scaleY, pivot = Offset.Zero)
    }) {
        drawPath(
            path,
            brush = Brush.verticalGradient(
                listOf(OilLight, OilDeep),
                startY = -24f * u,
                endY = 21.6f * u,
            ),
            alpha = alpha,
        )
        rotate(20f, pivot = Offset(-5.5f * u, 6f * u)) {
            drawOval(
                color = Color(0xFFFFF5DC),
                topLeft = Offset(-8.3f * u, 0.5f * u),
                size = Size(5.6f * u, 11f * u),
                alpha = 0.5f * alpha,
            )
        }
    }
}

// The seal's check, in "emblem units" (dp at scale 1, relative to the emblem
// centre): down-stroke then the long up-stroke.
private val CheckA = Offset(-27f, 1f)
private val CheckB = Offset(-8f, 20f)
private val CheckC = Offset(28f, -18f)
private val CheckFirstLeg = hypot(CheckB.x - CheckA.x, CheckB.y - CheckA.y)
private val CheckLength = CheckFirstLeg + hypot(CheckC.x - CheckB.x, CheckC.y - CheckB.y)

/** Radius, in dp, of the cream ring the check (or initial) sits in at scale 1. */
internal const val EMBLEM_RING_DP = 84f

/**
 * The seal's check centred on [center] at [scale] (1 = sized for the 84dp emblem
 * ring), drawn up to [progress] of its length.
 */
internal fun DrawScope.drawSealCheck(
    center: Offset,
    scale: Float,
    progress: Float,
    color: Color,
    alpha: Float = 1f,
) {
    if (progress <= 0f || alpha <= 0f) return
    val u = 1.dp.toPx() * scale
    fun p(o: Offset) = Offset(center.x + o.x * u, center.y + o.y * u)
    val drawn = CheckLength * progress
    val path = Path().apply {
        val a = p(CheckA)
        moveTo(a.x, a.y)
        if (drawn <= CheckFirstLeg) {
            val f = drawn / CheckFirstLeg
            val e = p(CheckA + (CheckB - CheckA) * f)
            lineTo(e.x, e.y)
        } else {
            val b = p(CheckB)
            lineTo(b.x, b.y)
            val f = (drawn - CheckFirstLeg) / (CheckLength - CheckFirstLeg)
            val e = p(CheckB + (CheckC - CheckB) * f)
            lineTo(e.x, e.y)
        }
    }
    drawPath(
        path,
        color = color,
        alpha = alpha,
        style = Stroke(width = 6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
