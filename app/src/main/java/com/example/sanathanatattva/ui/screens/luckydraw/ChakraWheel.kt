package com.example.sanathanatattva.ui.screens.luckydraw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import kotlin.math.roundToInt

/**
 * How the wheel moves.
 *  - [Still]: no motion.
 *  - [Spin]: driven, one turn per [SPIN_MS] — the loading state.
 *  - [Settle]: brake from wherever a spin is and land on a spoke, then rock back.
 *  - [Wait]: a ghani stone's pace, one turn per 16s — a result waiting to be opened.
 */
enum class WheelMotion { Still, Spin, Settle, Wait }

private const val SPIN_MS = 1100
private const val WAIT_MS = 16_000

/** Eight spokes: the wheel repeats every 45°, so any multiple is "aligned". */
private const val SPOKE_DEG = 45f
private const val SETTLE_TARGET_MS = 450f
private const val RECOIL_DEG = 7f
private const val RECOIL_MS = 260

// A cubic-bezier's launch rate is y1/x1 times its average, so knowing it lets the
// settle start at exactly the speed the wheel is already turning — no lurch or stall.
private val SettleEasing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private const val SETTLE_SLOPE = 1f / 0.33f
private val RecoilEasing = CubicBezierEasing(0.4f, 0f, 0.3f, 1f)

/**
 * The lucky draw's mark: a cart wheel, the ghani's own, with eight tapered spokes on a
 * cream face. The same drawing at every size, so the wheel that spins while a page loads
 * and the one resting on it afterwards are one object.
 */
@Composable
fun ChakraWheel(
    modifier: Modifier = Modifier,
    motion: WheelMotion = WheelMotion.Still,
    onSettled: () -> Unit = {},
) {
    val reduced = rememberReducedMotion()
    val angle = remember { Animatable(0f) }
    val settled by rememberUpdatedState(onSettled)
    LaunchedEffect(motion, reduced) {
        if (reduced) {
            angle.snapTo(0f)
            if (motion == WheelMotion.Settle) settled()
            return@LaunchedEffect
        }
        when (motion) {
            WheelMotion.Still -> Unit
            WheelMotion.Spin, WheelMotion.Wait -> {
                val turn = if (motion == WheelMotion.Spin) SPIN_MS else WAIT_MS
                while (true) {
                    // Continue from wherever the wheel is, so switching speed never jumps.
                    val from = angle.value % 360f
                    angle.snapTo(from)
                    angle.animateTo(from + 360f, tween(turn, easing = LinearEasing))
                }
            }
            WheelMotion.Settle -> {
                val from = angle.value % 360f
                angle.snapTo(from)
                val speed = 360f / SPIN_MS
                var target = ((from + speed * SETTLE_TARGET_MS / SETTLE_SLOPE) / SPOKE_DEG).roundToInt() * SPOKE_DEG
                if (target - from < 12f) target += SPOKE_DEG
                val duration = ((target - from) * SETTLE_SLOPE / speed).coerceIn(200f, 900f).toInt()
                // Past the mark, then back onto it: the weight in the hub taking up.
                angle.animateTo(target + RECOIL_DEG, tween(duration, easing = SettleEasing))
                angle.animateTo(target, tween(RECOIL_MS, easing = RecoilEasing))
                // On a spoke, the wheel is indistinguishable from an unrotated one.
                angle.snapTo(0f)
                settled()
            }
        }
    }
    Canvas(modifier.graphicsLayer { rotationZ = angle.value }) { drawWheel() }
}

private val Face = Color(0xFFF7F1E8)
private val RimStops = arrayOf(0f to Color(0xFF6B4A2C), 0.38f to Color(0xFF4A3020), 1f to Color(0xFF2B1B0E))
private val SpokeStops = arrayOf(
    0f to Color(0xFF4A2E14), 0.22f to Color(0xFF7A4B23), 0.5f to Color(0xFFA9793F),
    0.78f to Color(0xFF7A4B23), 1f to Color(0xFF4A2E14),
)
private val HubStops = arrayOf(0f to Color(0xFFB98F55), 0.55f to Color(0xFF8A5A2E), 1f to Color(0xFF4A2E14))

/** Geometry in a 100-unit box, light from the upper left. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWheel() {
    val u = size.minDimension / 100f
    val c = Offset(size.width / 2f, size.height / 2f)
    fun p(x: Float, y: Float) = Offset(c.x + (x - 50f) * u, c.y + (y - 50f) * u)
    drawCircle(Face, 39f * u, c)
    drawCircle(Brush.linearGradient(*RimStops, start = p(0f, 0f), end = p(100f, 100f)), 42.5f * u, c, style = Stroke(9f * u))
    drawCircle(Color(0xFF2B1B0E), 38.5f * u, c, alpha = 0.55f, style = Stroke(0.8f * u))
    drawCircle(Color(0xFF8A6039), 46.6f * u, c, alpha = 0.35f, style = Stroke(0.7f * u))
    // Broad at the nave, narrow at the felloe: a cart wheel, not a bicycle's.
    val spoke = Path().apply {
        val a = p(44.8f, 40.5f); moveTo(a.x, a.y)
        val b = p(55.2f, 40.5f); lineTo(b.x, b.y)
        val d = p(51.5f, 12f); lineTo(d.x, d.y)
        val e = p(48.5f, 12f); lineTo(e.x, e.y)
        close()
    }
    val grain = Brush.horizontalGradient(*SpokeStops, startX = p(44.8f, 0f).x, endX = p(55.2f, 0f).x)
    repeat(8) { i -> rotate(i * SPOKE_DEG, c) { drawPath(spoke, grain) } }
    drawCircle(Brush.radialGradient(*HubStops, center = p(46.55f, 45.4f), radius = 17.25f * u), 11.5f * u, c)
    drawCircle(Color(0xFF3A2410), 11.5f * u, c, alpha = 0.5f, style = Stroke(0.9f * u))
}

/**
 * The wheel turning inside a warm halo that breathes under it: the round page's loader.
 * Switch [motion] to [WheelMotion.Settle] when the wait is over; [onSettled] fires once
 * the wheel has stopped on a spoke (at once, with animations off).
 */
@Composable
fun WagonWheelLoader(
    size: Dp,
    motion: WheelMotion,
    modifier: Modifier = Modifier,
    wheelModifier: Modifier = Modifier,
    onSettled: () -> Unit = {},
) {
    val reduced = rememberReducedMotion()
    val breath = if (reduced) 0.5f else {
        val t = rememberInfiniteTransition(label = "Wheel halo")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "Halo breath").value
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 0.55f + 0.3f * breath
                    scaleX = 1f + 0.08f * breath
                    scaleY = scaleX
                }
                .drawWithCache {
                    val halo = Brush.radialGradient(
                        0f to Color(0x406A4423), 0.55f to Color(0x126A4423), 0.75f to Color.Transparent,
                        radius = this.size.minDimension / 2f,
                    )
                    onDrawBehind { drawCircle(halo) }
                },
        )
        ChakraWheel(wheelModifier.size(size * 0.63f), motion, onSettled)
    }
}

/** A small wheel turning in place of a button's label while its action runs. */
@Composable
fun MiniWheelSpinner(modifier: Modifier = Modifier, size: Dp = 18.dp) {
    ChakraWheel(modifier.size(size), WheelMotion.Spin)
}
