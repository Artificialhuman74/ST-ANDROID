package com.example.sanathanatattva.ui.screens.luckydraw

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.sanathanatattva.ui.brand.LightSystemBarIcons
import com.example.sanathanatattva.ui.brand.blockTouches
import com.example.sanathanatattva.ui.brand.drawOilDrop
import com.example.sanathanatattva.ui.brand.rememberOilDropPath
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.theme.EmblemSerif
import com.example.sanathanatattva.ui.theme.LogoCream
import com.example.sanathanatattva.ui.theme.PressGreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------
// The draw reveal, ported from the web's win moment. Everyone in the round watches
// the same ceremony; the gold lifts to the winner's name whether or not it's theirs.
//
// One authored clock, T in seconds, drives every beat as a pure function of T:
//   Waiting 0.0  the twelve month dots light clockwise, picking up speed
//   Drop    2.6  the chase lands on twelve and flashes; the dots turn to oil and fall in
//   Pour    4.4  oil rises inside the ring until the disc is full
//   Bloom   5.8  the pool becomes a molten disc that turns like the ghani stone
//   Reveal  7.4  gravity inverts: the gold lifts off and leaves the name behind
//   Settle 11.2  the reward line and the button arrive, and it holds there
//
// When the name is the viewer's own, the Crowning fires on top at 7.85s: three
// shockwave rings, a fountain of oil drops, slow sun rays behind the name, a camera
// kick, a shimmer across the name and a haptic triple-thud.
//
// The composition is authored on a 1080-wide design field (the web's), scaled to fit.
// ---------------------------------------------------------------------------

private const val TOTAL = 13.6f
private const val DROP = 2.6f
private const val POUR = 4.4f
private const val BLOOM = 5.8f
private const val REVEAL = 7.4f
private const val SETTLE = 11.2f
private const val CROWN = REVEAL + 0.45f

private const val CX = 540f
private const val CY = 900f
private const val R = 300f
/** The band that must always fit: the wordmark down to the button. */
private const val SAFE_H = 1404f
private const val ANCHOR_Y = 798f

private val Deep = Color(0xFF072318)
private val GreenLo = Color(0xFF0A2E1C)
private val Gold = Color(0xFFE0A73C)
private val GoldLo = Color(0xFFB07A1E)
private val GoldHi = Color(0xFFF7E3A1)
private val NameGold = Color(0xFFFBEFC8)

private fun easeOutCubic(t: Float) = (t - 1f).let { it * it * it + 1f }
private fun easeInOutQuart(t: Float) = if (t < 0.5f) 8f * t * t * t * t else 1f - 8f * (t - 1f).pow(4)
private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    return 1f + (c1 + 1f) * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
}

private inline fun tweenAt(t: Float, from: Float, to: Float, start: Float, end: Float, ease: (Float) -> Float): Float = when {
    t <= start -> from
    t >= end -> to
    else -> from + (to - from) * ease((t - start) / (end - start))
}

// The only three motion helpers: all easing goes through these, so it reads as one hand.
private fun enter(t: Float, from: Float, to: Float, start: Float, end: Float) = tweenAt(t, from, to, start, end, ::easeOutCubic)
private fun glide(t: Float, from: Float, to: Float, start: Float, end: Float) = tweenAt(t, from, to, start, end, ::easeInOutQuart)
private fun pop(t: Float, from: Float, to: Float, start: Float, end: Float) = tweenAt(t, from, to, start, end, ::easeOutBack)

/** Every scalar the beats need at one instant. */
private class Frame(val t: Float, val crowned: Boolean) {
    private val since = t - CROWN
    private val struck = crowned && since > 0f
    val kick = if (struck) exp(-since * 5f) else 0f
    val shakeX = if (struck) sin(since * 70f) * 16f * exp(-since * 6f) else 0f
    val camY = glide(t, 0f, -70f, BLOOM, REVEAL + 1.2f) + glide(t, 0f, 70f, SETTLE - 0.4f, TOTAL - 0.5f) +
        sin(t * 0.42f) * 6f + if (struck) cos(since * 55f) * 10f * exp(-since * 6f) else 0f
    val camScale = enter(t, 1.07f, 1f, 0f, POUR) + glide(t, 0f, 0.06f, BLOOM + 0.6f, REVEAL + 1.4f) +
        glide(t, 0f, -0.06f, SETTLE - 0.6f, TOTAL - 0.4f) + kick * 0.035f
    val seam = enter(t, 0f, 1f, 0f, 0.45f)
    val waitOut = enter(t, 1f, 0f, DROP - 0.2f, DROP + 0.8f)
    val chrome = 0.35f + 0.65f * enter(t, 1f, 0f, BLOOM, REVEAL)
    val ringFade = enter(t, 1f, 0f, BLOOM + 0.55f, BLOOM + 1.3f)
    val head = glide(t, 0f, 1f, 0.35f, DROP + 0.45f)
    val drain = enter(t, 0f, 1f, DROP + 0.5f, POUR + 1.7f)
    val level = enter(t, 0f, 1f, DROP + 0.62f, BLOOM - 0.1f)
    val flash = min(enter(t, 0f, 1f, DROP + 0.12f, DROP + 0.45f), enter(t, 1f, 0f, DROP + 0.45f, DROP + 1.4f))
    val halo = (0.25f + 0.75f * enter(t, 0f, 1f, BLOOM, REVEAL) + flash * 0.8f + kick * 0.6f).coerceAtMost(1f)
    val fill = enter(t, 0f, 1f, BLOOM, BLOOM + 1.1f)
    val evaporate = glide(t, 0f, 1f, REVEAL, REVEAL + 1.9f)
    val medScale = pop(t, 0.86f, 1f, BLOOM, BLOOM + 0.9f) * (1f + evaporate * 0.16f)
    val spokes = 0.5f * enter(t, 0f, 1f, BLOOM + 0.5f, REVEAL)
    val nameIn = enter(t, 0f, 1f, REVEAL + 0.55f, REVEAL + 2f)
    val kern = enter(t, 0.18f, 0.03f, REVEAL + 0.55f, REVEAL + 2.4f)
    val shine = if (crowned) exp(-((t - (CROWN + 1.4f)) / 0.45f).pow(2)) else 0f
    val sweep = enter(t, -0.3f, 1.3f, CROWN + 1f, CROWN + 2f)
    val whiteFlash = if (struck) 0.55f * max(0f, 1f - since / 0.4f) else 0f
    val rays = if (crowned) enter(t, 0f, 1f, CROWN, CROWN + 0.9f) * 0.26f * (1f - 0.5f * enter(t, 0f, 1f, SETTLE, SETTLE + 1.2f)) else 0f
    val rewardIn = enter(t, 0f, 1f, SETTLE - 0.6f, SETTLE + 0.4f)
    val button = pop(t, 0f, 1f, SETTLE + 0.25f, SETTLE + 1.1f).coerceIn(0f, 1f)
    val skippable = t > 1.2f && button < 0.5f
}

/** Maps design units on the 1080 field to this screen. */
private class Stage(val u: Float, val ox: Float, val oy: Float, private val density: Density) {
    fun offset(x: Float, y: Float) = IntOffset((ox + x * u).roundToInt(), (oy + y * u).roundToInt())
    fun dp(units: Float): Dp = with(density) { (units * u).toDp() }
    fun sp(units: Float): TextUnit = with(density) { (units * u).toSp() }
}

/**
 * The full-screen draw reveal. [onContinue] is the button at the end; [onBack] is a back
 * press once the piece has settled (a back press before that skips to the settled frame).
 */
@Composable
fun WinMoment(
    winnerName: String,
    isMe: Boolean,
    hasWinner: Boolean,
    schemeName: String,
    rewardProductName: String?,
    nextEntryDeadline: String?,
    hasNextDraw: Boolean,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = rememberReducedMotion()
    val clock = remember { Animatable(0f) }
    val skipped = remember { mutableStateOf(reduced) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val crowned = isMe && hasWinner
    LightSystemBarIcons(active = true)

    LaunchedEffect(Unit) {
        if (reduced) {
            clock.snapTo(TOTAL)
            view.buzz(ConfirmHaptic)
        } else {
            clock.animateTo(TOTAL, tween((TOTAL * 1000).toInt(), easing = LinearEasing))
        }
    }
    // Haptics stand in for the web's chime: a tap as the chase lands, a confirm as the
    // name arrives, and for the winner a triple thud under the shockwave.
    HapticCue(clock, DROP + 0.3f, skipped) { view.buzz(HapticFeedbackConstants.CLOCK_TICK) }
    HapticCue(clock, REVEAL + 0.6f, skipped) { view.buzz(ConfirmHaptic) }
    if (crowned) {
        for (beat in 0..2) HapticCue(clock, CROWN + beat * 0.12f, skipped) { view.buzz(HapticFeedbackConstants.LONG_PRESS) }
        HapticCue(clock, CROWN + 1.4f, skipped) { view.buzz(ConfirmHaptic) }
    }
    val settleNow = {
        if (!skipped.value) {
            skipped.value = true
            view.buzz(ConfirmHaptic)
        }
        scope.launch { clock.snapTo(TOTAL) }
        Unit
    }
    BackHandler { if (clock.value < TOTAL) settleNow() else onBack() }

    val frame = Frame(clock.value, crowned)
    val dropPath = rememberOilDropPath()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.radialGradient(
                    0f to PressGreen, 0.44f to GreenLo, 1f to Deep,
                    center = center, radius = max(size.width, size.height) * 0.75f,
                ))
            }
            .blockTouches()
            .pointerInput(Unit) {
                detectTapGestures { if (Frame(clock.value, crowned).skippable) settleNow() }
            },
    ) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val scale = min(min(maxWidth.value / 1080f, maxHeight.value / SAFE_H), 1.15f)
        val u = scale * density.density
        val stage = Stage(u, wPx / 2f - CX * u, hPx / 2f - ANCHOR_Y * u, density)

        // The halo sits behind the camera: it reads as light in the room.
        Canvas(Modifier.fillMaxSize()) {
            onStage(stage) {
                drawCircle(
                    Brush.radialGradient(
                        0f to Gold.copy(alpha = 0.30f), 0.46f to Gold.copy(alpha = 0.06f), 0.70f to Color.Transparent,
                        center = Offset(CX, CY), radius = 520f,
                    ),
                    radius = 520f, center = Offset(CX, CY), alpha = frame.halo,
                )
            }
        }

        // One continuous camera: settles in, pushes on the reveal, eases back out.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = frame.shakeX * u
                    translationY = frame.camY * u
                    scaleX = frame.camScale
                    scaleY = frame.camScale
                    transformOrigin = TransformOrigin(0.5f, (stage.oy + 960f * u) / hPx)
                },
        ) {
            Brandmark(stage, frame.chrome)
            WaitingTitle(stage, frame.waitOut, schemeName)
            Canvas(Modifier.fillMaxSize()) {
                onStage(stage) {
                    sunRays(frame)
                    monthDots(frame)
                    oilFill(frame)
                }
            }
            Medallion(stage, frame)
            Canvas(Modifier.fillMaxSize()) { onStage(stage) { shockwaves(frame) } }
            WinnerName(stage, frame, if (hasWinner) winnerName else "No one.", hasWinner)
            RewardLine(stage, frame, schemeName, rewardBody(isMe, hasWinner, rewardProductName, nextEntryDeadline, hasNextDraw))
            Canvas(Modifier.fillMaxSize()) {
                onStage(stage) {
                    sparks(frame)
                    fountain(frame, dropPath, density.density)
                }
            }
            Button(
                onClick = onContinue,
                enabled = frame.button > 0.5f,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold, contentColor = Color(0xFF2A1E05),
                    disabledContainerColor = Gold, disabledContentColor = Color(0xFF2A1E05),
                ),
                modifier = Modifier
                    .offset { stage.offset(140f, 1356f) }
                    .size(stage.dp(800f), stage.dp(132f))
                    .graphicsLayer {
                        alpha = frame.button
                        scaleX = 0.94f + 0.06f * frame.button
                        scaleY = scaleX
                    },
            ) {
                Text(if (crowned) "Claim your reward" else if (hasNextDraw) "Shop now to enter" else "Back to my round",
                    fontSize = stage.sp(44f), fontWeight = FontWeight.SemiBold)
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            // Vignette: keeps the eye on the circle.
            drawRect(Brush.radialGradient(
                0.52f to Color.Transparent, 1f to Color(0x8C020E09),
                center = Offset(size.width / 2f, size.height * 0.46f), radius = max(size.width, size.height) * 0.8f,
            ))
            if (frame.whiteFlash > 0f) drawRect(
                Brush.radialGradient(
                    0f to Color(0xFFFFF6D6), 0.4f to GoldHi.copy(alpha = 0.6f), 0.75f to Color.Transparent,
                    center = Offset(stage.ox + CX * u, stage.oy + CY * u), radius = max(size.width, size.height) * 0.8f,
                ),
                alpha = frame.whiteFlash,
            )
            // Open on the ground colour: the first frame is a held breath, not a hard cut.
            drawRect(Deep, alpha = 1f - frame.seam)
        }

        if (frame.skippable && frame.t < REVEAL) {
            Text(
                "TAP TO SKIP",
                color = LogoCream.copy(alpha = 0.4f),
                fontSize = 11.sp,
                letterSpacing = 0.28.em,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp),
            )
        }
    }
}

private val ConfirmHaptic =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY

private fun View.buzz(kind: Int) {
    performHapticFeedback(kind)
}

/** Fires [play] once when the clock passes [at], unless the piece was skipped past it. */
@Composable
private fun HapticCue(
    clock: Animatable<Float, AnimationVector1D>,
    at: Float,
    skipped: androidx.compose.runtime.State<Boolean>,
    play: () -> Unit,
) {
    LaunchedEffect(at) {
        snapshotFlow { clock.value >= at }.first { it }
        if (!skipped.value) play()
    }
}

/** Draws [block] in design units. */
private inline fun DrawScope.onStage(stage: Stage, block: DrawScope.() -> Unit) {
    withTransform({
        translate(stage.ox, stage.oy)
        scale(stage.u, stage.u, pivot = Offset.Zero)
    }, block)
}

// ── Beats drawn on the canvas (design units) ─────────────────────────────

private fun DrawScope.sunRays(f: Frame) {
    if (f.rays <= 0.001f) return
    val c = Offset(CX, 790f)
    val reach = 870f
    val brush = Brush.radialGradient(
        0f to GoldHi.copy(alpha = 0.55f), 0.08f to GoldHi.copy(alpha = 0.55f), 0.62f to Color.Transparent,
        center = c, radius = reach,
    )
    rotate(f.t * 7f, c) {
        repeat(12) { k ->
            drawArc(brush, startAngle = k * 30f - 90f, sweepAngle = 5f, useCenter = true,
                topLeft = Offset(c.x - reach, c.y - reach), size = Size(reach * 2, reach * 2), alpha = f.rays)
        }
    }
}

/** Twelve month markers: the chase lights them clockwise, then they turn to oil and fall in. */
private fun DrawScope.monthDots(f: Frame) {
    val outer = R + 96f
    val inner = R * 0.3f
    val headDeg = 30f + f.head * 330f
    for (i in 0 until 12) {
        val deg = if (i == 0) 360f else i * 30f // twelve o'clock arrives last
        val rev = ((headDeg - deg + 20f) / 20f).coerceIn(0f, 1f)
        if (rev <= 0.001f && f.drain <= 0f) continue
        val lit = rev * (0.5f + 0.5f * exp(-max(headDeg - deg, 0f) / 46f))
        val a = (-90f + deg) * PI.toFloat() / 180f
        val d = ((f.drain - i * 0.024f) / 0.74f).coerceIn(0f, 1f)
        val de = d * d * (3f - 2f * d)
        val radius = outer - (outer - inner) * de
        val x = CX + cos(a) * radius
        val y = CY + sin(a) * radius
        val s = (10f + lit * 12f + de * 14f) * (1f - de.pow(3.2f)) * (0.3f + 0.7f * rev)
        if (s <= 0.4f) continue
        val alpha = (max(rev * (0.45f + 0.55f * lit), de) * (0.9f + 0.1f * f.flash)).coerceIn(0f, 1f)
        val glow = 8f + lit * 40f + de * 40f + f.flash * 20f
        drawCircle(
            Brush.radialGradient(0f to Gold.copy(alpha = 0.2f + lit * 0.7f), 1f to Color.Transparent,
                center = Offset(x, y), radius = s / 2f + glow * 0.7f),
            radius = s / 2f + glow * 0.7f, center = Offset(x, y), alpha = alpha,
        )
        val body = Brush.radialGradient(
            0f to GoldHi, 0.58f to Gold, 1f to GoldLo,
            center = Offset(x - s * 0.14f, y - s * 0.2f), radius = s * 0.75f,
        )
        val stretch = 1f + de * 1.3f
        rotate(deg, Offset(x, y)) {
            drawOval(body, topLeft = Offset(x - s / 2f, y - s * stretch / 2f), size = Size(s, s * stretch), alpha = alpha)
        }
    }
}

private fun surface(x: Float, t: Float) =
    sin(x * 7f + t * 2.7f) * 0.030f + sin(x * 13.3f - t * 4.1f) * 0.014f +
        sin(x * 3.1f + t * 1.2f) * 0.020f + sin(x * 23f + t * 6.3f) * 0.006f

/** The drops become oil and the circle fills: a churning surface that calms as it rises. */
private fun DrawScope.oilFill(f: Frame) {
    if (f.ringFade <= 0.002f || f.level <= 0.002f) return
    val left = CX - R
    val top = CY - R
    val center = Offset(CX, CY)
    val calm = 1f - f.level.pow(1.4f)
    val fade = f.ringFade
    drawCircle(
        Brush.radialGradient(
            0f to Color.Transparent, (R - 8f) / (R + 70f) to Color.Transparent,
            R / (R + 70f) to Gold.copy(alpha = 0.4f * f.level), 1f to Color.Transparent,
            center = center, radius = R + 70f,
        ),
        radius = R + 70f, center = center, alpha = fade,
    )
    drawCircle(Gold, R, center, alpha = (0.1f + f.level * 0.9f) * fade, style = Stroke(2f))

    val crest = Path()
    val body = Path()
    val bandA = Path()
    val bandB = Path()
    var highest = top + 2 * R
    var x = 0f
    while (x <= 2 * R) {
        val ux = x / (2 * R)
        val lvl = -1.08f + 2.16f * f.level + surface(ux, f.t) * calm
        val y = top + R * (1f - lvl)
        highest = min(highest, y)
        if (x == 0f) {
            crest.moveTo(left, y); body.moveTo(left, y)
            bandA.moveTo(left, y + 70f + sin(ux * 9f + f.t * 1.6f) * 10f)
            bandB.moveTo(left, y + 170f + sin(ux * 6f - f.t * 1.2f) * 14f)
        } else {
            crest.lineTo(left + x, y); body.lineTo(left + x, y)
            bandA.lineTo(left + x, y + 70f + sin(ux * 9f + f.t * 1.6f) * 10f)
            bandB.lineTo(left + x, y + 170f + sin(ux * 6f - f.t * 1.2f) * 14f)
        }
        x += 12f
    }
    body.lineTo(left + 2 * R, top + 2 * R + 20f)
    body.lineTo(left, top + 2 * R + 20f)
    body.close()

    val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, R)) }
    clipPath(disc) {
        drawPath(body, Brush.verticalGradient(
            0f to GoldHi, 0.3f to Gold, 1f to Color(0xFF9A6A16), startY = highest, endY = top + 2 * R,
        ), alpha = fade)
        clipPath(body) {
            drawPath(bandA, Color(0xFFFFF0C8), alpha = 0.22f * fade, style = Stroke(5f))
            drawPath(bandB, Color(0xFFFFF0C8), alpha = 0.14f * fade, style = Stroke(4f))
            // A highlight up and to the left, and a darker rim: a filled lens.
            drawCircle(
                Brush.radialGradient(0f to Color(0x66FFF0C8), 1f to Color.Transparent,
                    center = Offset(left + 216f, top + 174f), radius = 260f),
                radius = 260f, center = Offset(left + 216f, top + 174f), alpha = f.level * fade,
            )
            drawCircle(
                Brush.radialGradient(0.72f to Color.Transparent, 1f to Color(0x4D000000), center = center, radius = R),
                radius = R, center = center, alpha = fade,
            )
        }
        drawPath(crest, GoldHi, alpha = 0.9f * fade, style = Stroke(7f))
        // Splash thrown off the crest while it's still churning.
        if (calm >= 0.12f) for (i in 0 until 12) {
            val sd = ((i * 7919) % 997) / 997f
            val sd2 = ((i * 6151) % 863) / 863f
            val cycle = 1.1f + sd2 * 0.9f
            val ph = ((f.t - sd * cycle) % cycle) / cycle
            if (ph < 0f || ph > 1f) continue
            val sz = 5f + sd2 * 7f
            val sx = left + 70f + sd * (2 * R - 140f)
            val sy = top + 2 * R - f.level * 2 * R - (ph * (60f + sd2 * 90f) - ph * ph * (90f + sd * 70f))
            drawOval(GoldHi, topLeft = Offset(sx - sz / 2f, sy), size = Size(sz, sz * (1f + ph * 0.8f)),
                alpha = (1f - ph) * calm * 0.9f * fade)
        }
    }
}

private fun DrawScope.shockwaves(f: Frame) {
    val since = f.t - CROWN
    if (!f.crowned || since <= 0f) return
    for (k in 0..2) {
        val p = (since - k * 0.16f) / 1.2f
        if (p <= 0f || p >= 1f) continue
        val radius = R * (1f + 2.4f * easeOutCubic(p))
        val width = (18f - k * 5f) * (1f - p) + 1.5f
        drawCircle(Gold, radius, Offset(CX, CY), alpha = 0.18f * (1f - p), style = Stroke(width + 46f))
        drawCircle(GoldHi, radius, Offset(CX, CY), alpha = (0.9f - k * 0.25f) * (1f - p).pow(1.3f), style = Stroke(width))
    }
}

/** Gold specks rising off the reveal. Deterministic from T, so it plays the same every time. */
private fun DrawScope.sparks(f: Frame) {
    val start = REVEAL - 0.1f
    val count = if (f.crowned) 40 else 26
    for (i in 0 until count) {
        val seed = ((i * 9301 + 49297) % 233280) / 233280f
        val seed2 = ((i * 4177 + 8191) % 65521) / 65521f
        val p = ((f.t - start - seed * 0.5f) / 2.6f).coerceIn(0f, 1f)
        if (p <= 0f || p >= 1f) continue
        val x = CX + (seed - 0.5f) * 620f * (0.4f + seed2) + sin(f.t * 1.6f + i * 2.3f) * 22f
        val y = CY + 120f - p * 900f * (0.5f + seed2) - seed2 * 40f
        val s = 4f + seed2 * 9f
        val c = Offset(x + s / 2f, y + s / 2f)
        drawCircle(Brush.radialGradient(0f to Gold.copy(alpha = 0.8f), 1f to Color.Transparent, center = c, radius = s + 8f),
            radius = s + 8f, center = c, alpha = 1f - p)
        drawCircle(if (seed2 > 0.6f) GoldHi else Gold, s / 2f, c, alpha = 1f - p)
    }
}

/** The Crowning's fountain: oil drops thrown up from the disc and falling back, tails trailing. */
private fun DrawScope.fountain(f: Frame, drop: Path, dpPx: Float) {
    val since = f.t - CROWN
    if (!f.crowned || since <= 0f) return
    val gravity = 3200f
    for (n in 0 until 44) {
        val h1 = ((n * 9301 + 49297) % 233280) / 233280f
        val h2 = ((n * 4177 + 8191) % 65521) / 65521f
        val h3 = ((n * 7919 + 104729) % 99991) / 99991f
        val dt = since - h1 * 0.25f
        val life = 2f + h2 * 0.8f
        if (dt <= 0f || dt >= life) continue
        val angle = (-90f + (h3 - 0.5f) * 140f) * PI.toFloat() / 180f
        val speed = 1500f + h2 * 1100f
        val vx = cos(angle) * speed
        val vy = sin(angle) * speed
        val x = CX + vx * dt
        val y = CY - 60f + vy * dt + 0.5f * gravity * dt * dt
        if (y > 2000f) continue
        val vyNow = vy + gravity * dt
        val height = 18f + h1 * 26f
        val k = height / (45.6f * dpPx)
        val stretch = 1f + min(hypot(vx, vyNow) / 2600f, 0.7f)
        val alpha = if (dt > life * 0.7f) 1f - (dt - life * 0.7f) / (life * 0.3f) else 1f
        // Tip up means falling; rotate so the tip always trails the direction of travel.
        rotate(atan2(vyNow, vx) * 180f / PI.toFloat() - 90f, Offset(x, y)) {
            drawOilDrop(drop, Offset(x, y), alpha = alpha, scaleX = k, scaleY = k * stretch)
        }
    }
}

// ── Beats that are type ──────────────────────────────────────────────────

@Composable
private fun Brandmark(stage: Stage, alpha: Float) {
    Column(
        Modifier.offset { stage.offset(0f, 128f) }.width(stage.dp(1080f)).graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SANATHANA TATTVA", color = LogoCream.copy(alpha = 0.5f), fontSize = stage.sp(28f),
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.34.em)
        Spacer(Modifier.height(stage.dp(18f)))
        Box(Modifier.size(stage.dp(64f), stage.dp(2f)).background(Gold.copy(alpha = 0.55f)))
    }
}

@Composable
private fun WaitingTitle(stage: Stage, alpha: Float, schemeName: String) {
    if (alpha <= 0.001f) return
    Column(
        Modifier.offset { stage.offset(0f, CY - 110f) }.width(stage.dp(1080f)).graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(LUCKY_DRAW_NAME.uppercase(), color = LogoCream.copy(alpha = 0.45f), fontSize = stage.sp(28f),
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.em)
        Spacer(Modifier.height(stage.dp(30f)))
        Text("Drawing now", color = LogoCream, fontFamily = EmblemSerif, fontWeight = FontWeight.SemiBold,
            fontSize = stage.sp(72f))
        Spacer(Modifier.height(stage.dp(22f)))
        Text(schemeName, color = LogoCream.copy(alpha = 0.5f), fontSize = stage.sp(34f), textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = stage.dp(160f)))
    }
}

/** The molten disc that turns like the ghani stone, then evaporates upward off the name. */
@Composable
private fun Medallion(stage: Stage, f: Frame) {
    if (f.fill <= 0.001f || f.evaporate >= 0.999f) return
    val u = stage.u
    Box(
        Modifier
            .offset { stage.offset(CX - R, CY - R) }
            .size(stage.dp(2 * R))
            .graphicsLayer {
                translationY = -f.evaporate * 300f * u
                scaleX = f.medScale
                scaleY = f.medScale
                alpha = (f.fill * (1f - f.evaporate.pow(1.5f))).coerceIn(0f, 1f)
                compositingStrategy = CompositingStrategy.Offscreen
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && f.evaporate > 0.01f) {
                    renderEffect = BlurEffect(f.evaporate * 26f * u, f.evaporate * 26f * u, TileMode.Decal)
                }
            }
            .drawBehind {
                val c = center
                val r = size.minDimension / 2f
                rotate(f.t * 60f, c) {
                    drawCircle(Brush.sweepGradient(
                        0f to GoldLo, 0.18f to GoldHi, 0.34f to Gold, 0.52f to GoldLo,
                        0.72f to GoldHi, 0.88f to Gold, 1f to GoldLo, center = c,
                    ), r, c)
                }
                rotate((f.t - BLOOM) * 26f, c) {
                    repeat(8) { k ->
                        drawArc(Color(0xFF78500A), startAngle = k * 45f - 90f, sweepAngle = 2f, useCenter = true,
                            topLeft = Offset.Zero, size = size, alpha = 0.2f * f.spokes)
                    }
                }
                drawCircle(Brush.radialGradient(0.7f to Color.Transparent, 1f to Color.White.copy(alpha = 0.28f),
                    center = c, radius = r), r, c)
                val reach = size.width * 0.963f
                drawCircle(Brush.radialGradient(0f to Color.White.copy(alpha = 0.55f), 0.46f to Color.Transparent,
                    center = Offset(size.width * 0.36f, size.height * 0.28f), radius = reach), r, c)
                if (f.evaporate > 0f) {
                    // The gold leaves from the bottom up, as if lifted off the page.
                    val cut = 100f - f.evaporate * 128f
                    val solid = (max(cut - 26f, 0f) / 100f).coerceIn(0f, 1f)
                    val clear = (max(cut, 2f) / 100f).coerceIn(solid, 1f)
                    drawRect(Brush.verticalGradient(0f to Color.Black, solid to Color.Black, clear to Color.Transparent),
                        blendMode = BlendMode.DstIn)
                }
            },
    )
}

/** The payoff: the name left behind where the gold was. Its spacing tightens as it lands. */
@Composable
private fun WinnerName(stage: Stage, f: Frame, name: String, hasWinner: Boolean) {
    if (f.nameIn <= 0.001f) return
    val u = stage.u
    val nameStyle = TextStyle(
        fontFamily = EmblemSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = stage.sp(96f),
        lineHeight = stage.sp(104f),
        letterSpacing = f.kern.em,
        textAlign = TextAlign.Center,
    ).let { base ->
        if (!f.crowned) base.copy(color = LogoCream) else {
            // A band of light crosses the winner's own name, and it glows.
            val band = 260f * u
            val x0 = f.sweep * 900f * u - band / 2f
            base.copy(
                brush = Brush.linearGradient(
                    listOf(NameGold, Color.White, NameGold),
                    start = Offset(x0, 0f), end = Offset(x0 + band, 0f), tileMode = TileMode.Clamp,
                ),
                shadow = Shadow(GoldHi.copy(alpha = 0.35f + 0.5f * f.shine), Offset.Zero, (18f + f.shine * 50f) * u),
            )
        }
    }
    Column(
        Modifier
            .offset { stage.offset(0f, CY - 210f) }
            .width(stage.dp(1080f))
            .graphicsLayer { alpha = f.nameIn }
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (hasWinner) "THIS MONTH'S WINNER" else "THIS MONTH'S DRAW",
            color = Gold.copy(alpha = 0.92f), fontSize = stage.sp(28f), fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.42.em,
        )
        Spacer(Modifier.height(stage.dp(34f)))
        Text(
            name,
            style = nameStyle,
            modifier = Modifier
                .padding(horizontal = stage.dp(90f))
                .graphicsLayer {
                    translationY = (1f - f.nameIn) * 26f * u
                    scaleX = 1f + f.shine * 0.03f
                    scaleY = scaleX
                },
        )
        Spacer(Modifier.height(stage.dp(44f)))
        Box(
            Modifier
                .size(stage.dp(120f + f.nameIn * 140f), stage.dp(3f))
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))),
        )
    }
}

@Composable
private fun RewardLine(stage: Stage, f: Frame, schemeName: String, body: androidx.compose.ui.text.AnnotatedString) {
    if (f.rewardIn <= 0.001f) return
    Column(
        Modifier
            .offset { stage.offset(120f, CY + 140f) }
            .width(stage.dp(840f))
            .graphicsLayer {
                alpha = f.rewardIn
                translationY = (1f - f.rewardIn) * 18f * stage.u
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(schemeName.uppercase(), color = Gold.copy(alpha = 0.85f), fontSize = stage.sp(28f),
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.34.em, textAlign = TextAlign.Center)
        Spacer(Modifier.height(stage.dp(26f)))
        Text(body, color = LogoCream.copy(alpha = 0.7f), fontSize = stage.sp(44f), lineHeight = stage.sp(64f),
            textAlign = TextAlign.Center)
    }
}

/** What the end of the piece says, which is the only part that differs between viewers. */
private fun rewardBody(
    isMe: Boolean,
    hasWinner: Boolean,
    product: String?,
    nextEntryDeadline: String?,
    hasNextDraw: Boolean,
) = buildAnnotatedString {
    val strong = SpanStyle(color = LogoCream, fontWeight = FontWeight.SemiBold)
    val deadline = drawDayMonth(nextEntryDeadline) ?: "the next draw"
    when {
        isMe && hasWinner -> {
            if (product != null) {
                append("We're delivering your ")
                withStyle(strong) { append(product) }
                append(" free, every month, for the rest of this round.")
            } else append("We're delivering your last order free, every month, for the rest of this round.")
        }
        !hasNextDraw -> append(
            if (hasWinner) "Not this time. That was the final draw of your round."
            else "The wheel stopped on an empty spoke. That was the final draw of your round.",
        )
        !hasWinner -> {
            append("No one ordered in time, so the wheel stopped on an empty spoke. Order before ")
            withStyle(strong) { append(deadline) }
            append(" and the next one could carry your name.")
        }
        else -> {
            append("Not this month. Order before ")
            withStyle(strong) { append(deadline) }
            append(" to be in the next draw.")
        }
    }
}
