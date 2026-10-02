package com.example.sanathanatattva.ui.brand

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.sanathanatattva.ui.components.EaseOutQuint
import com.example.sanathanatattva.ui.components.ExitAccelerateEasing
import com.example.sanathanatattva.ui.components.GravityEasing
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.theme.EmblemSerif
import com.example.sanathanatattva.ui.theme.LogoCream
import com.example.sanathanatattva.ui.theme.OilDeep
import com.example.sanathanatattva.ui.theme.OilGold
import com.example.sanathanatattva.ui.theme.OilLight
import com.example.sanathanatattva.ui.theme.PressGreen
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Gold rim left around a medallion landing, in dp (matches [OrderSealMedallion]). */
internal const val SEAL_RIM_DP = 3f

/**
 * The beats of one moment, in ms. Only the opening differs by origin (a pill
 * narrows first; a spinner closes its ring first); everything after is keyed
 * off [flood], the instant the drop lands.
 */
private class SealTiming(val morphStart: Int, val fallStart: Int, val flood: Int) {
    val ripples = flood + 10
    val covered = flood + 620
    val ring = flood + 260
    val drops = flood + 400
    val emblem = flood + 420
    val firstLine = flood + 600
    val collapse = flood + 1700
    val collapseEnd = collapse + 450
    val ping = collapse + 400
    val total = ping + 500

    companion object {
        fun of(origin: SealOrigin) = when (origin) {
            is SealOrigin.Pill -> SealTiming(morphStart = 280, fallStart = 540, flood = 830)
            is SealOrigin.Spinner -> SealTiming(morphStart = 200, fallStart = 480, flood = 760)
        }
    }
}

/** Where the green goes, resolved into this overlay's own coordinates. */
private sealed interface Landing {
    data class IntoRect(val rect: Rect) : Landing
    data class Settle(val center: Offset, val radius: Float) : Landing
    data object Fold : Landing
}

private const val FLOOD_OPEN_MS = 600
private const val COLLAPSE_MS = 450
private const val RING_DROPS = 12

@Composable
internal fun SealOverlay(
    moment: SealMoment,
    tabBounds: (String) -> Rect?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var covered by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val cover = {
        if (!covered) {
            covered = true
            moment.onCovered()
        }
    }
    val finish = {
        if (!finished) {
            finished = true
            cover()
            onFinished()
        }
    }

    if (rememberReducedMotion()) {
        LaunchedEffect(Unit) { finish() }
        return
    }

    val density = LocalDensity.current
    val timing = remember(moment) { SealTiming.of(moment.origin) }
    val clock = rememberMomentClock(timing.total, finish)
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    var landing by remember { mutableStateOf<Landing?>(null) }

    OnCue(clock, timing.covered, cover)
    OnCue(clock, timing.collapse) {
        landing = resolveLanding(moment.landing(), tabBounds, rootOffset, density)
    }
    // A medallion or a fold is done once it lands; only a tab waits for its ping.
    OnCue(clock, timing.collapseEnd) { if (landing !is Landing.IntoRect) finish() }

    val onGreen by remember {
        derivedStateOf { clock.value in (timing.flood + 150f)..(timing.collapse + 300f) }
    }
    LightSystemBarIcons(active = onGreen)
    BackHandler { /* the moment is short; let it finish */ }

    val ringDrop = rememberRingDropPath()
    val measurer = rememberTextMeasurer()
    val styles = SealTextStyles(
        eyebrow = MaterialTheme.typography.bodyLarge.copy(color = LogoCream.copy(alpha = 0.8f)),
        headline = TextStyle(
            fontFamily = EmblemSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 44.sp,
            color = LogoCream,
        ),
        initial = TextStyle(
            fontFamily = EmblemSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            color = OilGold,
        ),
    )
    val pingColor = MaterialTheme.colorScheme.primary

    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.positionInRoot() }
            .blockTouches()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = listOfNotNull(moment.eyebrow, moment.headline, moment.caption)
                    .joinToString(" ")
            },
    ) {
        // 1 · the control → drop → fall, and the gold rim that leads the flood.
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            val geo = SealGeometry(size, moment.origin.bounds.translate(-rootOffset), this)
            drawOrigin(moment.origin, geo, timing, t)
            val flood = geo.flood(t, timing, landing)
            if (flood.rim > 0f) drawCircle(OilGold, flood.rim, flood.center)
        }
        // 2 · the flood, clipped to its circle, carrying the seal and the message.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val geo = SealGeometry(size, moment.origin.bounds.translate(-rootOffset), this)
                    val flood = geo.flood(clock.value, timing, landing)
                    alpha = if (flood.radius > 0f) 1f else 0f
                    clip = true
                    shape = CircleAt(flood.center, flood.radius)
                }
                .background(PressGreen),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val t = clock.value
                val geo = SealGeometry(size, moment.origin.bounds.translate(-rootOffset), this)
                drawSeal(moment, geo, timing, landing, t, ringDrop, measurer, styles)
            }
        }
        // 3 · ripples where the drop lands, and the ping on the tab it folds into.
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            val geo = SealGeometry(size, moment.origin.bounds.translate(-rootOffset), this)
            drawLandingRipples(geo.impact, timing, t)
            (landing as? Landing.IntoRect)?.let { drawTabPing(it.rect, timing, t, pingColor) }
        }
    }
}

private class SealTextStyles(val eyebrow: TextStyle, val headline: TextStyle, val initial: TextStyle)

/** A circle at an arbitrary point — the flood's clip. */
private class CircleAt(private val center: Offset, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(Path().apply { addOval(Rect(center, max(radius, 0f))) })
}

private class FloodState(val center: Offset, val radius: Float, val rim: Float)

/** Static positions for one frame, plus the flood's position over time. */
private class SealGeometry(val size: Size, val origin: Rect, density: Density) {
    private val dp = density.density
    /** Diameter of the circle the control becomes before turning into a drop. */
    val startDiameter = min(origin.width, origin.height)
    /** How far the drop lifts as it forms, before it falls. */
    val lift = 8f * dp
    val restCenter = Offset(origin.center.x, origin.center.y - lift)
    val impact = Offset(
        origin.center.x,
        max(size.height * 0.83f, origin.center.y + 96f * dp).coerceAtMost(size.height - 24f * dp),
    )
    val fallDistance = impact.y - restCenter.y
    private val floodMax = hypot(
        max(impact.x, size.width - impact.x),
        max(impact.y, size.height - impact.y),
    ) + 24f * dp
    /** Centre of the ring of drops and the message under it. */
    val stage = Offset(size.width / 2f, size.height * 0.40f)
    val ringRadius = EMBLEM_RING_DP * dp

    fun flood(t: Float, timing: SealTiming, landing: Landing?): FloodState {
        val open = phase(t, timing.flood, FLOOD_OPEN_MS, EaseOutQuint)
        val openRadius = floodMax * open
        val openRim = openRadius + 14f * dp * open
        if (t < timing.collapse || landing == null) return FloodState(impact, openRadius, openRim)
        val settle = landing is Landing.Settle
        val c = phase(t, timing.collapse, COLLAPSE_MS, if (settle) EaseOutQuint else ExitAccelerateEasing)
        val (target, radius, rim) = when (landing) {
            is Landing.IntoRect -> Triple(landing.rect.center, 0f, 0f)
            is Landing.Settle -> Triple(landing.center, landing.radius, landing.radius + SEAL_RIM_DP * dp)
            Landing.Fold -> Triple(stage, 0f, 0f)
        }
        return FloodState(
            center = androidx.compose.ui.geometry.lerp(impact, target, c),
            radius = lerp(openRadius, radius, c),
            rim = lerp(openRim, rim, c),
        )
    }
}

private fun resolveLanding(
    requested: SealLanding?,
    tabBounds: (String) -> Rect?,
    rootOffset: Offset,
    density: Density,
): Landing = when (requested) {
    is SealLanding.Tab -> tabBounds(requested.route)?.let { Landing.IntoRect(it.translate(-rootOffset)) }
        ?: Landing.Fold
    is SealLanding.Medallion -> Landing.Settle(
        center = requested.center - rootOffset,
        radius = with(density) { requested.radius.toPx() },
    )
    null -> Landing.Fold
}

// ── 1 · origin → drop → fall ────────────────────────────────────────────────

private fun DrawScope.drawOrigin(origin: SealOrigin, geo: SealGeometry, timing: SealTiming, t: Float) {
    if (t >= timing.fallStart + 300) return
    val morph = phase(t, timing.morphStart, 260, EaseOutQuint)
    val fall = phase(t, timing.fallStart, 300, GravityEasing)
    when (origin) {
        is SealOrigin.Pill -> {
            if (morph <= 0f) {
                // The pill narrows to a circle around its own centre.
                val d = geo.startDiameter
                val w = lerp(geo.origin.width, d, phase(t, 0, 320, EaseOutQuint))
                drawRoundRect(
                    color = origin.fill,
                    topLeft = Offset(geo.origin.center.x - w / 2f, geo.origin.center.y - d / 2f),
                    size = Size(w, d),
                    cornerRadius = CornerRadius(d / 2f),
                )
            } else {
                drawDropMorph(geo, timing, t, morph, fall, fill = origin.fill, ring = null, ringAlpha = 0f)
            }
        }
        is SealOrigin.Spinner -> {
            // The spinning arc gives way to a closed ring, which then fills with oil.
            val closing = phase(t, 0, 200)
            if (closing < 1f) {
                val r = geo.startDiameter / 2f - 2.dp.toPx()
                drawArc(
                    color = origin.tint,
                    startAngle = t * 0.36f - 90f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = geo.origin.center - Offset(r, r),
                    size = Size(r * 2f, r * 2f),
                    alpha = 1f - closing,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            drawDropMorph(geo, timing, t, morph, fall, fill = null, ring = origin.tint, ringAlpha = closing)
        }
    }
}

/**
 * A rounded square whose top-left corner sharpens while it turns 45° — a circle
 * becoming a teardrop — then free-falls, stretching, to the impact point.
 */
private fun DrawScope.drawDropMorph(
    geo: SealGeometry,
    timing: SealTiming,
    t: Float,
    morph: Float,
    fall: Float,
    fill: Color?,
    ring: Color?,
    ringAlpha: Float,
) {
    val d = geo.startDiameter
    val half = d / 2f
    val s = lerp(1f, 46.dp.toPx() / d, morph)
    val fade = 1f - phase(t, timing.fallStart + 282, 18)
    val center = Offset(
        geo.origin.center.x,
        geo.origin.center.y - geo.lift * morph + geo.fallDistance * fall,
    )
    fun shape(inset: Float) = Path().apply {
        val h = half - inset
        val round = CornerRadius(h)
        addRoundRect(
            RoundRect(
                left = -h, top = -h, right = h, bottom = h,
                topLeftCornerRadius = CornerRadius(lerp(h, 3.dp.toPx(), morph)),
                topRightCornerRadius = round,
                bottomRightCornerRadius = round,
                bottomLeftCornerRadius = round,
            ),
        )
    }
    withTransform({
        translate(center.x, center.y)
        scale(lerp(1f, 0.9f, fall) * s, lerp(1f, 1.16f, fall) * s, pivot = Offset.Zero)
        rotate(45f * morph, pivot = Offset.Zero)
    }) {
        val body = shape(0f)
        if (fill != null) drawPath(body, fill, alpha = (1f - morph) * fade)
        drawPath(
            body,
            brush = Brush.linearGradient(listOf(OilLight, OilDeep), Offset(-half, -half), Offset(half, half)),
            alpha = morph * fade,
        )
        if (ring != null && ringAlpha > 0f) {
            drawPath(
                shape(2.dp.toPx()),
                color = lerp(ring, OilGold, morph),
                alpha = ringAlpha * fade,
                style = Stroke(width = 4.dp.toPx()),
            )
        }
    }
}

// ── 2 · the seal inside the flood ───────────────────────────────────────────

private fun DrawScope.drawSeal(
    moment: SealMoment,
    geo: SealGeometry,
    timing: SealTiming,
    landing: Landing?,
    t: Float,
    ringDrop: Path,
    measurer: TextMeasurer,
    styles: SealTextStyles,
) {
    val collapsing = landing != null && t >= timing.collapse
    val settle = landing as? Landing.Settle
    // Settling: the ring, drops and words fade while the emblem itself flies home.
    // Folding away: everything shrinks toward the landing point and fades.
    val detailAlpha = if (collapsing && settle != null) 1f - phase(t, timing.collapse - 50, 200) else 1f
    val fold = if (collapsing && settle == null) phase(t, timing.collapse, 300, ExitAccelerateEasing) else 0f
    val foldPivot = when (landing) {
        is Landing.IntoRect -> landing.rect.center
        else -> geo.stage
    }
    val land = if (settle != null) phase(t, timing.collapse, COLLAPSE_MS, EaseOutQuint) else 0f
    val emblemCenter = if (settle != null) androidx.compose.ui.geometry.lerp(geo.stage, settle.center, land) else geo.stage
    val emblemScale = if (settle != null) lerp(1f, settle.radius / geo.ringRadius, land) else 1f

    val s = lerp(1f, 0.2f, fold)
    scale(s, s, pivot = foldPivot) {
        val a = (1f - fold)
        drawRingOfDrops(geo, timing, t, ringDrop, a * detailAlpha)
        drawEmblem(moment.emblem, emblemCenter, emblemScale, timing, t, measurer, styles.initial, a)
        drawMessage(moment, geo, timing, t, measurer, styles, a * detailAlpha)
    }
}

/** The cream ring draws itself, then twelve drops light up around it, clockwise. */
private fun DrawScope.drawRingOfDrops(geo: SealGeometry, timing: SealTiming, t: Float, drop: Path, alpha: Float) {
    if (alpha <= 0f) return
    val r = geo.ringRadius
    val ring = phase(t, timing.ring, 500, EaseOutQuint)
    if (ring > 0f) {
        drawArc(
            color = LogoCream,
            startAngle = -90f,
            sweepAngle = 360f * ring,
            useCenter = false,
            topLeft = geo.stage - Offset(r, r),
            size = Size(r * 2f, r * 2f),
            alpha = alpha,
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
    val dropRadius = 102.dp.toPx()
    val settleDistance = 6.dp.toPx()
    for (i in 0 until RING_DROPS) {
        val p = phase(t, timing.drops + 45 * i, 300, EaseOutQuint)
        if (p <= 0f) continue
        val angle = Math.toRadians(30.0 * i)
        val at = Offset(
            geo.stage.x + (sin(angle) * dropRadius).toFloat(),
            geo.stage.y - (cos(angle) * dropRadius).toFloat(),
        )
        withTransform({
            translate(at.x, at.y)
            rotate(30f * i, pivot = Offset.Zero)
            translate(0f, -settleDistance * (1f - p))
            scale(lerp(0.3f, 1f, p), pivot = Offset.Zero)
        }) {
            drawPath(drop, OilGold, alpha = p * alpha)
        }
    }
}

private fun DrawScope.drawEmblem(
    emblem: SealEmblem,
    center: Offset,
    emblemScale: Float,
    timing: SealTiming,
    t: Float,
    measurer: TextMeasurer,
    initialStyle: TextStyle,
    alpha: Float,
) {
    if (alpha <= 0f) return
    when (emblem) {
        SealEmblem.Check -> drawSealCheck(
            center = center,
            scale = emblemScale,
            progress = phase(t, timing.emblem, 400, EaseOutQuint),
            color = OilGold,
            alpha = alpha,
        )
        is SealEmblem.Initial -> {
            val p = phase(t, timing.emblem, 450, EaseOutQuint)
            if (p <= 0f) return
            val layout = measurer.measure(emblem.letter, initialStyle)
            val s = lerp(0.6f, 1f, p) * emblemScale
            scale(s, s, pivot = center) {
                drawText(
                    layout,
                    topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f),
                    alpha = p * alpha,
                )
            }
        }
    }
}

/** Eyebrow, headline, caption — stacked under the ring, each rising into place in turn. */
private fun DrawScope.drawMessage(
    moment: SealMoment,
    geo: SealGeometry,
    timing: SealTiming,
    t: Float,
    measurer: TextMeasurer,
    styles: SealTextStyles,
    alpha: Float,
) {
    if (alpha <= 0f) return
    val side = 24.dp.toPx()
    val width = (size.width - side * 2f).toInt().coerceAtLeast(1)
    val lines = listOfNotNull(
        moment.eyebrow?.let { it to styles.eyebrow },
        moment.headline to styles.headline,
        moment.caption?.let { it to styles.eyebrow },
    )
    var y = geo.stage.y + 130.dp.toPx()
    lines.forEachIndexed { index, (text, style) ->
        val layout = measurer.measure(
            text,
            style.copy(textAlign = TextAlign.Center),
            constraints = Constraints.fixedWidth(width),
        )
        val p = phase(t, timing.firstLine + 100 * index, 500, EaseOutQuint)
        if (p > 0f) {
            drawText(
                layout,
                topLeft = Offset(side, y + (1f - p) * 10.dp.toPx()),
                alpha = p * alpha,
            )
        }
        y += layout.size.height + 4.dp.toPx()
    }
}

// ── 3 · ripples and the tab ping ────────────────────────────────────────────

private fun DrawScope.drawLandingRipples(at: Offset, timing: SealTiming, t: Float) {
    listOf(timing.ripples to LogoCream, timing.ripples + 80 to OilGold).forEach { (start, color) ->
        val raw = ((t - start) / 520f).coerceIn(0f, 1f)
        if (raw <= 0f || raw >= 1f) return@forEach
        val alpha = if (raw < 0.1f) raw * 9f else 0.9f * (1f - raw) / 0.9f
        val s = lerp(0.4f, 3.2f, EaseOutQuint.transform(raw))
        val rx = 24.dp.toPx()
        val ry = 6.dp.toPx()
        scale(s, s, pivot = at) {
            drawOval(
                color = color,
                topLeft = at - Offset(rx, ry),
                size = Size(rx * 2f, ry * 2f),
                alpha = alpha,
                style = Stroke(width = 1.5.dp.toPx() / s),
            )
        }
    }
}

/** One expanding ring around the tab the moment folded into — "it lives here now". */
private fun DrawScope.drawTabPing(tab: Rect, timing: SealTiming, t: Float, color: Color) {
    val p = phase(t, timing.ping, 500, EaseOutQuint)
    if (p <= 0f || p >= 1f) return
    val pill = Rect(tab.left + 3.dp.toPx(), tab.top, tab.right - 3.dp.toPx(), tab.bottom)
    val s = lerp(1f, 1.35f, p)
    scale(s, s, pivot = pill.center) {
        drawRoundRect(
            color = color,
            topLeft = pill.topLeft,
            size = pill.size,
            cornerRadius = CornerRadius(22.dp.toPx()),
            alpha = 0.9f * (1f - p),
            style = Stroke(width = 2.dp.toPx() / s),
        )
    }
}
