package com.example.sanathanatattva.ui.screens.containers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// The "why steel" story: a five-frame swipeable intro shown once. Each frame
// plays a one-shot spotlit choreography — the scene lights up, the threat
// travels in, steel deflects it, the verdicts stamp in — then rests, fully
// readable. Swiping back to a frame replays its beat.
// ---------------------------------------------------------------------------

private data class StoryFrame(
    val headline: String,
    val body: String,
    val steelVerdict: String?,
    val plasticVerdict: String?,
)

private val frames = listOf(
    StoryFrame(
        headline = "Sunlight ruins good oil",
        body = "Light turns cold-pressed oil rancid. Steel blocks every ray. Plastic lets it through.",
        steelVerdict = "Light blocked ✓",
        plasticVerdict = "Light gets in ✗",
    ),
    StoryFrame(
        headline = "Air seeps through plastic",
        body = "Oxygen slips through plastic walls in weeks. Steel is sealed tight.",
        steelVerdict = "Sealed ✓",
        plasticVerdict = "Air seeps in ✗",
    ),
    StoryFrame(
        headline = "Plastic ends up in the oil",
        body = "Oil slowly pulls chemicals out of plastic. Steel adds nothing at all.",
        steelVerdict = "Nothing added ✓",
        plasticVerdict = "Leaches ✗",
    ),
    StoryFrame(
        headline = "Old smells stay in plastic",
        body = "Plastic holds odours and passes them to the next oil. Steel stays neutral.",
        steelVerdict = "Stays neutral ✓",
        plasticVerdict = "Holds odours ✗",
    ),
    StoryFrame(
        headline = "So your oil lives in steel",
        body = "The container is yours to keep refilling. Return it any time and the deposit comes back.",
        steelVerdict = null,
        plasticVerdict = null,
    ),
)

/** One-shot playback length per frame. */
private const val PLAY_MS = 2600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteelStorySheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(pageCount = { frames.size })
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    val onLastPage = pagerState.currentPage == frames.lastIndex

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StoryProgress(
                    count = frames.size,
                    current = pagerState.currentPage,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) {
                    // Keep the width stable so the progress bar doesn't jump on the last frame.
                    Text(if (onLastPage) " " else "Skip")
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val pageOffset =
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                StoryFrameContent(
                    page = page,
                    frame = frames[page],
                    // Play the beat the moment this frame becomes the target page, not
                    // once it has fully settled. The beat opens from a dark spotlit
                    // t = 0; triggering on settle meant an already-centred, fully-drawn
                    // frame would blink back to that dark scrim and replay — the "shadow
                    // before the card" flicker. Starting on currentPage folds that dark
                    // open into the incoming slide, where the motion hides it.
                    play = pagerState.currentPage == page,
                    reducedMotion = reducedMotion,
                    pageOffset = pageOffset,
                )
            }

            Button(
                onClick = {
                    if (onLastPage) {
                        onDismiss()
                    } else {
                        scope.launch {
                            if (reducedMotion) {
                                pagerState.scrollToPage(pagerState.currentPage + 1)
                            } else {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(if (onLastPage) "Got it" else "Next")
            }
        }
    }
}

@Composable
private fun StoryProgress(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        color = if (i <= current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun StoryFrameContent(
    page: Int,
    frame: StoryFrame,
    play: Boolean,
    reducedMotion: Boolean,
    pageOffset: Float,
) {
    // Master clock: one linear 0→1 sweep each time this frame becomes the target
    // page; each element carves its own eased segment out of it. Rest state (t = 1)
    // is the fully composed, readable scene — what a frame you've left rests at, and
    // what reduced motion shows immediately.
    val clock = remember { Animatable(1f) }
    LaunchedEffect(play, reducedMotion) {
        if (play && !reducedMotion) {
            clock.snapTo(0f)
            clock.animateTo(1f, tween(PLAY_MS, easing = LinearEasing))
        } else {
            clock.snapTo(1f)
        }
    }
    val t = clock.value

    val sceneColors = storySceneColors()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    // Gentle parallax: the scene trails the swipe slightly. The alpha
                    // dip is kept shallow so the incoming frame stays clearly a frame
                    // (not a faded ghost) while it slides in and its beat opens.
                    translationX = pageOffset * size.width * 0.25f
                    alpha = 1f - 0.18f * abs(pageOffset).coerceAtMost(1f)
                },
        ) {
            drawStoryScene(page, t, sceneColors)
        }

        if (frame.steelVerdict != null && frame.plasticVerdict != null) {
            Row(Modifier.fillMaxWidth()) {
                VerdictStamp(
                    text = frame.steelVerdict,
                    color = MaterialTheme.colorScheme.primary,
                    t = t,
                    start = 0.56f,
                    end = 0.72f,
                    modifier = Modifier.weight(1f),
                )
                VerdictStamp(
                    text = frame.plasticVerdict,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    t = t,
                    start = 0.70f,
                    end = 0.86f,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        Text(
            frame.headline,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                val p = seg(t, 0.02f, 0.20f)
                alpha = p
                translationY = (1f - p) * 10.dp.toPx()
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            frame.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                val p = seg(t, 0.08f, 0.28f)
                alpha = p
                translationY = (1f - p) * 10.dp.toPx()
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** A verdict label that stamps in: scales down from 1.35× with a settle. */
@Composable
private fun VerdictStamp(
    text: String,
    color: Color,
    t: Float,
    start: Float,
    end: Float,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier.graphicsLayer {
            val p = linSeg(t, start, end)
            alpha = seg(t, start, end)
            val s = 1.35f + (1f - 1.35f) * overshoot(p)
            scaleX = s
            scaleY = s
        },
    )
}

// ---------------------------------------------------------------------------
// Scene drawing
// ---------------------------------------------------------------------------

private data class SceneColors(
    val threat: Color,      // ink-muted: particles, wisps, flecks
    val good: Color,        // brand green: the protective signal on steel
    val scrim: Color,       // spotlight vignette
)

@Composable
private fun storySceneColors() = SceneColors(
    threat = MaterialTheme.colorScheme.onSurfaceVariant,
    good = MaterialTheme.colorScheme.primary,
    scrim = MaterialTheme.colorScheme.onSurface,
)

// Illustration pigments (image-like constants, not UI roles).
private val SteelEdge = Color(0xFF5D666D)
private val SteelDark = Color(0xFF6E767D)
private val SteelMid = Color(0xFF9AA2A8)
private val SteelBright = Color(0xFFE9EDF0)
private val SteelSoft = Color(0xFFC3CACF)
private val PlasticWall = Color(0xFFB9BEC2)
private val PlasticCap = Color(0xFF9BA1A6)
private val OilTop = Color(0xFFC2A45C)
private val OilDeep = Color(0xFF98803F)
private val OilRancid = Color(0xFF6E6136)
private val SunWarm = Color(0xFFE6C97E)
private val BeamWarm = Color(0xFFD9BC6F)
private val LabelCream = Color(0xFFF3E9CE)
private val LabelGreen = Color(0xFF14532D)

// --- Geometry -------------------------------------------------------------

/**
 * The shop's milk-can style steel canister: wide cylindrical body, flared
 * shoulder stepping into a narrower neck, overhanging flat lid with a strap
 * handle, and a wire swing handle at the neck.
 */
private class CanGeom(val rect: Rect) {
    val cx = rect.center.x
    val neckW = rect.width * 0.54f
    val lidW = neckW * 1.16f
    val lidH = rect.height * 0.048f
    val neckTop = rect.top + rect.height * 0.115f
    val shoulderTop = rect.top + rect.height * 0.235f
    val bodyTop = rect.top + rect.height * 0.345f
    val baseR = rect.width * 0.08f

    /** Where a light ray glances off the shoulder. */
    val hit = Offset(cx + rect.width * 0.21f, rect.top + rect.height * 0.29f)

    val bodyPath = Path().apply {
        moveTo(cx - neckW / 2f, neckTop)
        lineTo(cx - neckW / 2f, shoulderTop)
        quadraticTo(rect.left + rect.width * 0.03f, shoulderTop + rect.height * 0.035f, rect.left, bodyTop)
        lineTo(rect.left, rect.bottom - baseR)
        quadraticTo(rect.left, rect.bottom, rect.left + baseR, rect.bottom)
        lineTo(rect.right - baseR, rect.bottom)
        quadraticTo(rect.right, rect.bottom, rect.right, rect.bottom - baseR)
        lineTo(rect.right, bodyTop)
        quadraticTo(rect.right - rect.width * 0.03f, shoulderTop + rect.height * 0.035f, cx + neckW / 2f, shoulderTop)
        lineTo(cx + neckW / 2f, neckTop)
        close()
    }
}

/** A PET oil bottle: cap, neck, shoulder, straight body, rounded base. */
private class BottleGeom(val rect: Rect) {
    val cx = rect.center.x
    val capW = rect.width * 0.34f
    val capH = rect.height * 0.07f
    val neckW = rect.width * 0.28f
    val neckBottom = rect.top + capH + rect.height * 0.06f
    val shoulderBottom = rect.top + rect.height * 0.30f
    val baseR = rect.width * 0.16f
    val oilTop = rect.top + rect.height * 0.36f

    val outline = Path().apply {
        moveTo(cx - neckW / 2f, rect.top + capH)
        lineTo(cx - neckW / 2f, neckBottom)
        quadraticTo(rect.left + rect.width * 0.02f, neckBottom + rect.height * 0.04f, rect.left, shoulderBottom)
        lineTo(rect.left, rect.bottom - baseR)
        quadraticTo(rect.left, rect.bottom, rect.left + baseR, rect.bottom)
        lineTo(rect.right - baseR, rect.bottom)
        quadraticTo(rect.right, rect.bottom, rect.right, rect.bottom - baseR)
        lineTo(rect.right, shoulderBottom)
        quadraticTo(rect.right - rect.width * 0.02f, neckBottom + rect.height * 0.04f, cx + neckW / 2f, neckBottom)
        lineTo(cx + neckW / 2f, rect.top + capH)
        close()
    }
}

private fun DrawScope.drawStoryScene(page: Int, t: Float, colors: SceneColors) {
    if (page == frames.lastIndex) {
        drawFinaleScene(t, colors)
    } else {
        val canW = size.width * 0.34f
        val canH = size.height * 0.52f
        val bottleW = size.width * 0.28f
        val bottleH = size.height * 0.55f
        val baseY = size.height * 0.88f
        val can = CanGeom(
            Rect(Offset(size.width * 0.25f - canW / 2f, baseY - canH), Size(canW, canH)),
        )
        val bottle = BottleGeom(
            Rect(Offset(size.width * 0.75f - bottleW / 2f, baseY - bottleH), Size(bottleW, bottleH)),
        )

        drawGroundShadow(can.rect, colors)
        drawGroundShadow(bottle.rect, colors)
        drawSteelCan(can)
        drawPlasticBottle(bottle, rancid = if (page == 0) seg(t, 0.52f, 0.78f) else 0f)

        when (page) {
            0 -> drawLightScene(can, bottle, t, colors)
            1 -> drawAirScene(can, bottle, t, colors)
            2 -> drawLeachScene(can, bottle, t, colors)
            3 -> drawOdourScene(can, bottle, t, colors)
        }
    }
    drawSpotlight(page, seg(t, 0f, 0.16f), colors)
}

// --- Vessels ----------------------------------------------------------------

private fun DrawScope.drawGroundShadow(rect: Rect, colors: SceneColors) {
    val cy = rect.bottom + rect.height * 0.015f
    drawOval(
        color = colors.scrim.copy(alpha = 0.07f),
        topLeft = Offset(rect.center.x - rect.width * 0.62f, cy - rect.height * 0.030f),
        size = Size(rect.width * 1.24f, rect.height * 0.060f),
    )
    drawOval(
        color = colors.scrim.copy(alpha = 0.08f),
        topLeft = Offset(rect.center.x - rect.width * 0.44f, cy - rect.height * 0.020f),
        size = Size(rect.width * 0.88f, rect.height * 0.040f),
    )
}

private fun metalBrush(left: Float, right: Float) = Brush.horizontalGradient(
    0.00f to SteelEdge,
    0.14f to SteelMid,
    0.26f to SteelBright,
    0.36f to SteelSoft,
    0.55f to SteelMid,
    0.82f to SteelDark,
    1.00f to SteelEdge,
    startX = left,
    endX = right,
)

private fun DrawScope.drawSteelCan(can: CanGeom, withLabel: Boolean = false) {
    val r = can.rect

    // Wire swing handle first, so it sits behind the body at its pivot.
    val pivot = Offset(can.cx + can.neckW / 2f, (can.neckTop + can.shoulderTop) / 2f)
    val handle = Path().apply {
        moveTo(pivot.x, pivot.y)
        quadraticTo(
            r.right + r.width * 0.26f, pivot.y + r.height * 0.03f,
            r.right + r.width * 0.09f, r.top + r.height * 0.56f,
        )
    }
    drawPath(handle, color = SteelEdge, style = Stroke(width = 7f, cap = StrokeCap.Round))
    drawPath(handle, color = SteelSoft, style = Stroke(width = 3f, cap = StrokeCap.Round))

    // Body + shoulder + neck in one silhouette, cylindrical shading.
    drawPath(can.bodyPath, brush = metalBrush(r.left, r.right))

    // Overhanging flat lid with a strap handle on top.
    drawRoundRect(
        brush = metalBrush(can.cx - can.lidW / 2f, can.cx + can.lidW / 2f),
        topLeft = Offset(can.cx - can.lidW / 2f, can.neckTop - can.lidH),
        size = Size(can.lidW, can.lidH),
        cornerRadius = CornerRadius(can.lidH * 0.45f),
    )
    val strapR = r.width * 0.11f
    val strapC = Offset(can.cx, can.neckTop - can.lidH)
    drawArc(
        color = SteelDark,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(strapC.x - strapR, strapC.y - strapR),
        size = Size(strapR * 2f, strapR * 2f),
        style = Stroke(width = 6f, cap = StrokeCap.Round),
    )

    // Seams: under the lid, at the neck-shoulder joint, and where the
    // shoulder meets the body.
    drawLine(
        color = SteelEdge.copy(alpha = 0.5f),
        start = Offset(can.cx - can.neckW / 2f + 2f, can.shoulderTop),
        end = Offset(can.cx + can.neckW / 2f - 2f, can.shoulderTop),
        strokeWidth = 2f,
    )
    drawLine(
        color = SteelEdge.copy(alpha = 0.55f),
        start = Offset(r.left + 2f, can.bodyTop + 4f),
        end = Offset(r.right - 2f, can.bodyTop + 4f),
        strokeWidth = 2.5f,
    )
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(r.left + 3f, can.bodyTop + 7f),
        end = Offset(r.right - 3f, can.bodyTop + 7f),
        strokeWidth = 1.5f,
    )

    if (withLabel) {
        drawCanLabel(can)
    }

    // Ambient occlusion pooling at the base.
    clipPath(can.bodyPath) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.14f),
                startY = r.bottom - r.height * 0.16f,
                endY = r.bottom,
            ),
            topLeft = Offset(r.left, r.bottom - r.height * 0.16f),
            size = Size(r.width, r.height * 0.16f),
        )
    }
}

/** The product label: cream field over a deep-green band, kept abstract. */
private fun DrawScope.drawCanLabel(can: CanGeom) {
    val r = can.rect
    val labelW = r.width * 0.68f
    val labelTop = can.bodyTop + r.height * 0.09f
    val labelH = r.height * 0.42f
    val label = Rect(Offset(can.cx - labelW / 2f, labelTop), Size(labelW, labelH))
    val labelPath = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(label, CornerRadius(10f)))
    }
    drawPath(labelPath, color = LabelCream)
    clipPath(labelPath) {
        drawRect(
            color = LabelGreen,
            topLeft = Offset(label.left, label.top + labelH * 0.62f),
            size = Size(labelW, labelH * 0.38f),
        )
        // Faint text-like strokes so it reads as a printed label.
        drawLine(
            color = LabelGreen.copy(alpha = 0.5f),
            start = Offset(label.left + labelW * 0.22f, label.top + labelH * 0.30f),
            end = Offset(label.right - labelW * 0.22f, label.top + labelH * 0.30f),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = LabelGreen.copy(alpha = 0.30f),
            start = Offset(label.left + labelW * 0.30f, label.top + labelH * 0.42f),
            end = Offset(label.right - labelW * 0.30f, label.top + labelH * 0.42f),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
    }
    // Cylindrical shading carries over the paper.
    drawPath(
        labelPath,
        brush = Brush.horizontalGradient(
            0.00f to Color.Black.copy(alpha = 0.18f),
            0.25f to Color.Transparent,
            0.45f to Color.White.copy(alpha = 0.12f),
            0.75f to Color.Transparent,
            1.00f to Color.Black.copy(alpha = 0.22f),
            startX = label.left,
            endX = label.right,
        ),
    )
}

private fun DrawScope.drawPlasticBottle(bottle: BottleGeom, rancid: Float) {
    val r = bottle.rect

    // Translucent wall.
    drawPath(bottle.outline, color = Color.White.copy(alpha = 0.40f))

    // Oil inside: vertical gradient, darker when rancid, with a meniscus.
    val oilA = lerpColor(OilTop, OilRancid, rancid * 0.4f)
    val oilB = lerpColor(OilDeep, OilRancid, rancid * 0.4f)
    clipPath(bottle.outline) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to oilA.copy(alpha = 0.92f),
                1f to oilB.copy(alpha = 0.95f),
                startY = bottle.oilTop,
                endY = r.bottom,
            ),
            topLeft = Offset(r.left, bottle.oilTop),
            size = Size(r.width, r.bottom - bottle.oilTop),
        )
        // Meniscus: the oil surface reads as an ellipse.
        drawOval(
            color = Color.White.copy(alpha = 0.30f),
            topLeft = Offset(r.left + r.width * 0.06f, bottle.oilTop - r.height * 0.014f),
            size = Size(r.width * 0.88f, r.height * 0.028f),
        )
        // Wall sheen: one soft vertical highlight along the left side.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(r.left + r.width * 0.12f, bottle.shoulderBottom),
            size = Size(r.width * 0.07f, r.bottom - bottle.shoulderBottom - r.height * 0.06f),
            cornerRadius = CornerRadius(r.width * 0.035f),
        )
    }

    drawPath(bottle.outline, color = PlasticWall, style = Stroke(width = 3f))

    // Ridged cap.
    val capLeft = bottle.cx - bottle.capW / 2f
    drawRoundRect(
        color = PlasticCap,
        topLeft = Offset(capLeft, r.top),
        size = Size(bottle.capW, bottle.capH),
        cornerRadius = CornerRadius(bottle.capH * 0.25f),
    )
    repeat(4) { i ->
        val x = capLeft + bottle.capW * (0.2f + 0.2f * i)
        drawLine(
            color = Color.Black.copy(alpha = 0.15f),
            start = Offset(x, r.top + bottle.capH * 0.2f),
            end = Offset(x, r.top + bottle.capH * 0.8f),
            strokeWidth = 1.5f,
        )
    }
}

// --- Spotlight ---------------------------------------------------------------

/**
 * The spotlight: the scene starts dimmed with a tight pool of light, then the
 * pool opens up. A faint vignette stays at rest so each frame keeps its stage.
 */
private fun DrawScope.drawSpotlight(page: Int, spot: Float, colors: SceneColors) {
    val focus = when (page) {
        0 -> Offset(size.width * 0.5f, size.height * 0.30f)
        frames.lastIndex -> center
        else -> Offset(size.width * 0.5f, size.height * 0.55f)
    }
    val wash = lerp(0.07f, 0f, spot)
    if (wash > 0.004f) {
        drawRect(color = colors.scrim.copy(alpha = wash))
    }
    val radius = lerp(size.minDimension * 0.55f, size.maxDimension * 1.35f, spot)
    val edge = lerp(0.16f, 0.045f, spot)
    drawRect(
        brush = Brush.radialGradient(
            0.45f to Color.Transparent,
            1f to colors.scrim.copy(alpha = edge),
            center = focus,
            radius = radius,
        ),
    )
}

// --- Frame 1: light ----------------------------------------------------------

/** Sun beams: deflected by the steel shoulder, refracted into the plastic's oil. */
private fun DrawScope.drawLightScene(can: CanGeom, bottle: BottleGeom, t: Float, colors: SceneColors) {
    val sun = Offset(size.width * 0.5f, size.height * 0.10f)
    val sunR = size.width * 0.045f
    val sunIn = seg(t, 0.04f, 0.16f)
    val reachSteel = seg(t, 0.14f, 0.40f)
    val reachPlastic = seg(t, 0.20f, 0.46f)
    val deflect = overshoot(linSeg(t, 0.46f, 0.62f))
    val bloom = seg(t, 0.52f, 0.78f)

    if (sunIn > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to SunWarm.copy(alpha = 0.35f * sunIn),
                1f to SunWarm.copy(alpha = 0f),
                center = sun,
                radius = sunR * 2.6f,
            ),
            radius = sunR * 2.6f,
            center = sun,
        )
        drawCircle(SunWarm.copy(alpha = 0.95f * sunIn), radius = sunR, center = sun)
        repeat(8) { i ->
            val a = i / 8f * 2f * PI.toFloat()
            val dir = Offset(cos(a), sin(a))
            drawLine(
                SunWarm.copy(alpha = 0.8f * sunIn),
                start = sun + dir * (sunR * 1.4f),
                end = sun + dir * (sunR * (1.4f + 0.5f * sunIn)),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        }
    }

    // Steel: the beam lands on the shoulder and stops there.
    if (reachSteel > 0f) {
        drawBeam(sun, can.hit, fraction = reachSteel, color = BeamWarm)
    }
    if (deflect > 0f) {
        // Glint at the point of impact, plus a bounced beam leaving the surface.
        val glintR = 7f + 7f * deflect
        drawGlint(can.hit, glintR, alpha = (0.85f * deflect).coerceIn(0f, 1f))
        val out = Offset(-0.72f, -0.70f)
        drawBeam(
            can.hit,
            can.hit + out * (size.width * 0.16f),
            fraction = deflect.coerceAtMost(1.2f).coerceAtLeast(0f),
            color = BeamWarm,
            startWidth = 9f,
            endWidth = 3f,
            alpha = 0.55f,
        )
    }

    // Plastic: the beam passes the wall and lands in the oil.
    val wallPoint = Offset(bottle.cx - bottle.rect.width * 0.30f, bottle.rect.top + bottle.rect.height * 0.34f)
    val oilPoint = Offset(bottle.cx - bottle.rect.width * 0.02f, bottle.rect.top + bottle.rect.height * 0.62f)
    if (reachPlastic > 0f) {
        val toWall = (reachPlastic / 0.7f).coerceAtMost(1f)
        drawBeam(sun, wallPoint, fraction = toWall, color = BeamWarm)
        if (reachPlastic > 0.7f) {
            val inside = (reachPlastic - 0.7f) / 0.3f
            // Slightly bent and dimmer inside the bottle: refraction.
            drawBeam(wallPoint, oilPoint, fraction = inside, color = BeamWarm, alpha = 0.35f, startWidth = 10f, endWidth = 14f)
        }
    }
    if (bloom > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to OilRancid.copy(alpha = 0.65f * bloom),
                1f to OilRancid.copy(alpha = 0f),
                center = oilPoint,
                radius = 16f + 26f * bloom,
            ),
            radius = 16f + 26f * bloom,
            center = oilPoint,
        )
    }
}

/** A translucent wedge of light from [from] toward [to], drawn to [fraction]. */
private fun DrawScope.drawBeam(
    from: Offset,
    to: Offset,
    fraction: Float,
    color: Color,
    startWidth: Float = 5f,
    endWidth: Float = 13f,
    alpha: Float = 0.5f,
) {
    if (fraction <= 0f) return
    val f = fraction.coerceAtMost(1f)
    val end = from + (to - from) * f
    val angle = atan2(to.y - from.y, to.x - from.x)
    val perp = Offset(-sin(angle), cos(angle))
    val w0 = startWidth / 2f
    val w1 = (startWidth + (endWidth - startWidth) * f) / 2f
    val path = Path().apply {
        moveTo(from.x + perp.x * w0, from.y + perp.y * w0)
        lineTo(end.x + perp.x * w1, end.y + perp.y * w1)
        lineTo(end.x - perp.x * w1, end.y - perp.y * w1)
        lineTo(from.x - perp.x * w0, from.y - perp.y * w0)
        close()
    }
    drawPath(
        path,
        brush = Brush.linearGradient(
            0f to color.copy(alpha = alpha * 0.5f),
            1f to color.copy(alpha = alpha),
            start = from,
            end = end,
        ),
    )
}

/** A four-point specular glint. */
private fun DrawScope.drawGlint(center: Offset, r: Float, alpha: Float) {
    drawCircle(Color.White.copy(alpha = alpha), radius = r * 0.35f, center = center)
    drawLine(
        Color.White.copy(alpha = alpha),
        start = center + Offset(-r, 0f), end = center + Offset(r, 0f),
        strokeWidth = 2.5f, cap = StrokeCap.Round,
    )
    drawLine(
        Color.White.copy(alpha = alpha),
        start = center + Offset(0f, -r), end = center + Offset(0f, r),
        strokeWidth = 2.5f, cap = StrokeCap.Round,
    )
}

// --- Frame 2: air --------------------------------------------------------------

/** Air bubbles: bounce off the steel wall, drift straight through the plastic one. */
private fun DrawScope.drawAirScene(can: CanGeom, bottle: BottleGeom, t: Float, colors: SceneColors) {
    val n = 6
    repeat(n) { i ->
        val d = i * 0.045f
        val bodySpan = can.rect.bottom - can.bodyTop
        val y = can.bodyTop + bodySpan * (0.10f + 0.80f * i / (n - 1f))
        val radius = 5f + 2.5f * ((i * 0.37f) % 1f)

        // Steel side: fly in from the left, bounce back off the wall.
        val travel = seg(t, 0.12f + d, 0.38f + d)
        val bounce = seg(t, 0.38f + d, 0.58f + d)
        if (travel > 0f) {
            val startX = can.rect.left - size.width * 0.17f
            val wallX = can.rect.left - radius - 3f
            val x = lerp(startX, wallX, travel) - bounce * 34f
            drawBubble(Offset(x, y - bounce * 6f), radius, colors)
        }

        // Plastic side: fly in from the right and pass straight through the wall.
        val travel2 = seg(t, 0.12f + d, 0.52f + d)
        if (travel2 > 0f) {
            val startX = bottle.rect.right + size.width * 0.17f
            val endX = bottle.cx + bottle.rect.width * 0.10f
            val x = lerp(startX, endX, travel2)
            val inside = x < bottle.rect.right - 3f
            drawBubble(Offset(x, y), radius, colors, alpha = if (inside) 0.55f else 1f)
        }
    }
}

private fun DrawScope.drawBubble(center: Offset, r: Float, colors: SceneColors, alpha: Float = 1f) {
    drawCircle(colors.threat.copy(alpha = 0.28f * alpha), radius = r, center = center)
    drawCircle(colors.threat.copy(alpha = 0.75f * alpha), radius = r, center = center, style = Stroke(width = 1.8f))
    drawCircle(Color.White.copy(alpha = 0.65f * alpha), radius = r * 0.28f, center = center + Offset(-r * 0.3f, -r * 0.3f))
}

// --- Frame 3: leaching -----------------------------------------------------------

/** Flecks pull free of the plastic's inner wall and hang in the oil; steel gleams once. */
private fun DrawScope.drawLeachScene(can: CanGeom, bottle: BottleGeom, t: Float, colors: SceneColors) {
    drawSheen(can, linSeg(t, 0.40f, 0.70f))

    val r = bottle.rect
    val n = 8
    repeat(n) { i ->
        val d = i * 0.04f
        val p = seg(t, 0.24f + d, 0.62f + d)
        if (p <= 0f) return@repeat
        val fromRight = i % 2 == 0
        val originX = if (fromRight) r.right - 6f else r.left + 6f
        val originY = bottle.oilTop + (r.bottom - bottle.oilTop) * (0.15f + 0.75f * ((i * 0.23f) % 1f))
        val driftX = (if (fromRight) -1f else 1f) * r.width * (0.22f + 0.12f * ((i * 0.41f) % 1f))
        val pos = Offset(originX + driftX * p, originY + r.height * 0.08f * p)
        val s = 5f + 4f * ((i * 0.29f) % 1f)
        rotate(degrees = p * (if (fromRight) -50f else 50f), pivot = pos) {
            drawRoundRect(
                color = colors.threat.copy(alpha = lerp(0.9f, 0.55f, p)),
                topLeft = pos,
                size = Size(s, s * 0.7f),
                cornerRadius = CornerRadius(1.5f),
            )
        }
    }
}

/** One bright band sweeping the steel body — plays only mid-animation. */
private fun DrawScope.drawSheen(can: CanGeom, p: Float) {
    if (p <= 0f || p >= 1f) return
    val r = can.rect
    clipPath(can.bodyPath) {
        val bandW = r.width * 0.30f
        val x = lerp(r.left - bandW, r.right + bandW, p)
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.White.copy(alpha = 0f),
                0.5f to Color.White.copy(alpha = 0.55f),
                1f to Color.White.copy(alpha = 0f),
                startX = x,
                endX = x + bandW,
            ),
            topLeft = Offset(x, r.top),
            size = Size(bandW, r.height),
        )
    }
}

// --- Frame 4: odour ----------------------------------------------------------------

/** Odour wisps: curl away from steel, soak into the plastic wall and stay there. */
private fun DrawScope.drawOdourScene(can: CanGeom, bottle: BottleGeom, t: Float, colors: SceneColors) {
    val travel = seg(t, 0.12f, 0.48f)
    val pop = overshoot(linSeg(t, 0.48f, 0.64f))
    val stain = seg(t, 0.52f, 0.78f)

    repeat(2) { i ->
        val y = can.bodyTop + (can.rect.bottom - can.bodyTop) * (0.22f + 0.42f * i)
        val wobble = i * 1.7f

        // Steel: the wisp stops at the wall.
        if (travel > 0f) {
            drawWisp(
                startX = can.rect.left - size.width * 0.19f,
                endX = can.rect.left - 6f,
                y = y,
                wobblePhase = wobble,
                t = t,
                fraction = travel,
                color = colors.threat,
            )
        }
        // A deflection arc pops once the wisp arrives.
        if (pop > 0f) {
            val arcR = 16f * pop
            drawArc(
                color = colors.good.copy(alpha = 0.75f * pop.coerceAtMost(1f)),
                startAngle = 110f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(can.rect.left - 6f - arcR, y - arcR),
                size = Size(arcR * 2f, arcR * 2f),
                style = Stroke(width = 3f, cap = StrokeCap.Round),
            )
        }

        // Plastic: the wisp sinks into the wall; a stain lingers.
        if (travel > 0f) {
            drawWisp(
                startX = bottle.rect.right + size.width * 0.19f,
                endX = bottle.rect.right + 4f,
                y = y,
                wobblePhase = wobble + 1f,
                t = t,
                fraction = travel,
                color = colors.threat,
            )
        }
        if (stain > 0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to colors.threat.copy(alpha = 0.45f * stain),
                    1f to colors.threat.copy(alpha = 0f),
                    center = Offset(bottle.rect.right - 6f, y),
                    radius = 12f + 12f * stain,
                ),
                radius = 12f + 12f * stain,
                center = Offset(bottle.rect.right - 6f, y),
            )
        }
    }
}

/** A smoke-like curl whose amplitude grows along its length; drawn to [fraction]. */
private fun DrawScope.drawWisp(
    startX: Float,
    endX: Float,
    y: Float,
    wobblePhase: Float,
    t: Float,
    fraction: Float,
    color: Color,
) {
    val steps = 24
    val last = (steps * fraction).toInt().coerceAtLeast(1)
    val path = Path()
    for (s in 0..last) {
        val f = s / steps.toFloat()
        val x = lerp(startX, endX, f)
        val amp = 9f * (0.4f + 0.8f * f)
        val wy = y + sin(f * 4f * PI.toFloat() + wobblePhase + t * 5f) * amp
        if (s == 0) path.moveTo(x, wy) else path.lineTo(x, wy)
    }
    drawPath(path, color = color.copy(alpha = 0.6f), style = Stroke(width = 4f, cap = StrokeCap.Round))
}

// --- Frame 5: finale ------------------------------------------------------------------

/** One steel canister takes the stage: pops in, gleams once, earns its check. */
private fun DrawScope.drawFinaleScene(t: Float, colors: SceneColors) {
    val pop = overshoot(linSeg(t, 0.06f, 0.34f))
    if (pop <= 0f) return
    val sweep = linSeg(t, 0.40f, 0.72f)
    val badgePop = overshoot(linSeg(t, 0.66f, 0.84f))
    val tickIn = seg(t, 0.74f, 0.88f)

    val vesselW = size.width * 0.42f
    val vesselH = size.height * 0.60f
    val baseY = size.height * 0.86f
    val can = CanGeom(
        Rect(Offset(size.width / 2f - vesselW / 2f, baseY - vesselH), Size(vesselW, vesselH)),
    )

    scale(scale = 0.90f + 0.10f * pop, pivot = Offset(size.width / 2f, baseY)) {
        drawGroundShadow(can.rect, colors)
        drawSteelCan(can, withLabel = true)
        drawSheen(can, sweep)
    }

    // Brand check badge stamps onto the shoulder.
    if (badgePop > 0f) {
        val badgeC = Offset(can.rect.right - can.rect.width * 0.06f, can.bodyTop + can.rect.height * 0.02f)
        val badgeR = can.rect.width * 0.14f * badgePop
        drawCircle(colors.good, radius = badgeR, center = badgeC)
        if (tickIn > 0f) {
            val fullR = can.rect.width * 0.14f
            val tick = Path().apply {
                moveTo(badgeC.x - fullR * 0.42f, badgeC.y + fullR * 0.02f)
                lineTo(badgeC.x - fullR * 0.10f, badgeC.y + fullR * 0.34f)
                lineTo(badgeC.x + fullR * 0.46f, badgeC.y - 0.30f * fullR)
            }
            drawPath(
                tick,
                color = Color.White.copy(alpha = tickIn),
                style = Stroke(width = 5f, cap = StrokeCap.Round),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Timeline + math helpers
// ---------------------------------------------------------------------------

/** Eased progress of [t] through the [start]..[end] segment of the timeline. */
private fun seg(t: Float, start: Float, end: Float): Float {
    if (t <= start) return 0f
    if (t >= end) return 1f
    return FastOutSlowInEasing.transform((t - start) / (end - start))
}

/** Linear (unclamped-easing) progress through a segment; feed this to [overshoot]. */
private fun linSeg(t: Float, start: Float, end: Float): Float =
    ((t - start) / (end - start)).coerceIn(0f, 1f)

/** Ease-out-back: overshoots 1 slightly, then settles. */
private fun overshoot(p: Float, tension: Float = 2.2f): Float {
    if (p <= 0f) return 0f
    if (p >= 1f) return 1f
    val q = p - 1f
    return q * q * ((tension + 1f) * q + tension) + 1f
}

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f

private fun lerpColor(a: Color, b: Color, f: Float) = Color(
    red = lerp(a.red, b.red, f),
    green = lerp(a.green, b.green, f),
    blue = lerp(a.blue, b.blue, f),
    alpha = lerp(a.alpha, b.alpha, f),
)

private operator fun Offset.times(scale: Float) = Offset(x * scale, y * scale)
