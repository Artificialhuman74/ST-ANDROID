package com.example.sanathanatattva.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The Add button that turns into a − count + stepper. One fixed-height control
 * whose pill stretches and pinches into two droplets as the first unit lands
 * (and flows back together when the count returns to zero).
 *
 * Everything that moves is read only in the layout and draw phases, so the
 * morph never recomposes the card and the card never changes height. The curve
 * only decelerates: no spring, no overshoot.
 */
@Composable
fun CartStepper(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    canIncrement: Boolean = true,
) {
    val reducedMotion = rememberReducedMotion()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val fill = if (tonal) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary
    val onFill = if (tonal) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
    val labelStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)

    val height = 44.dp
    val stepperWidth = 128.dp
    val measurer = rememberTextMeasurer()
    val pillWidthPx = remember(label, labelStyle, density) {
        with(density) { measurer.measure(label, labelStyle).size.width + (2 * 22).dp.toPx() }
    }
    val stepperWidthPx = with(density) { stepperWidth.toPx() }

    // 0 = one pill showing the label, 1 = two droplets with the count between.
    val split = remember { Animatable(if (count > 0) 1f else 0f) }
    LaunchedEffect(count > 0, reducedMotion) {
        val target = if (count > 0) 1f else 0f
        if (reducedMotion) split.snapTo(target)
        else split.animateTo(target, tween(if (target == 1f) 420 else 340, easing = EaseOutQuint))
    }

    Box(
        modifier
            .layout { measurable, constraints ->
                val w = (pillWidthPx + (stepperWidthPx - pillWidthPx) * split.value).roundToInt()
                    .coerceIn(constraints.minWidth, constraints.maxWidth)
                val h = height.roundToPx()
                val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w, minHeight = h, maxHeight = h))
                layout(w, h) { placeable.place(0, 0) }
            }
            .drawBehind { drawDroplets(split.value, fill) },
    ) {
        // Label, fading out as the pill parts.
        Box(
            Modifier.fillMaxSize().clearAndSetSemantics {}.graphicsLayer { alpha = (1f - split.value * 2.5f).coerceIn(0f, 1f) },
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = labelStyle, color = onFill, maxLines = 1)
        }

        // The count, welling up in the gap once the droplets have parted.
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val a = ((split.value - 0.55f) / 0.45f).coerceIn(0f, 1f)
                alpha = a
                scaleX = 0.7f + 0.3f * a
                scaleY = scaleX
            },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = count.coerceAtLeast(1),
                transitionSpec = {
                    if (reducedMotion) fadeIn(snap()) togetherWith fadeOut(snap())
                    else {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInVertically(tween(200, easing = EaseOutQuint)) { h -> dir * h / 2 } + fadeIn(tween(160))) togetherWith
                            (slideOutVertically(tween(180, easing = EaseOutQuint)) { h -> -dir * h / 2 } + fadeOut(tween(120)))
                    }
                },
                label = "stepperCount",
            ) { c ->
                Text("$c", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (count <= 0) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClickLabel = label) {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onIncrement()
                    }
                    .semantics { contentDescription = label },
            )
        } else {
            StepperEnd(
                icon = Icons.Filled.Remove,
                description = "Remove one $label",
                tint = onFill,
                split = split,
                alignment = Alignment.CenterStart,
                onClick = {
                    haptics.performHapticFeedback(if (count == 1) HapticFeedbackType.Reject else HapticFeedbackType.SegmentTick)
                    onDecrement()
                },
            )
            StepperEnd(
                icon = Icons.Filled.Add,
                description = "Add one $label",
                tint = if (canIncrement) onFill else onFill.copy(alpha = 0.45f),
                split = split,
                alignment = Alignment.CenterEnd,
                enabled = canIncrement,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onIncrement()
                },
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.StepperEnd(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    split: Animatable<Float, *>,
    alignment: Alignment,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        Modifier
            .align(alignment)
            .width(44.dp)
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .graphicsLayer { alpha = ((split.value - 0.25f) / 0.5f).coerceIn(0f, 1f) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

/**
 * One body of liquid: a pill at [p] = 0; as [p] rises the middle pinches into a
 * neck that thins to nothing, leaving two round droplets at the ends.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDroplets(p: Float, color: Color) {
    val h = size.height
    val r = h / 2f
    val cy = r
    if (p <= 0.001f) {
        drawRoundRect(color, cornerRadius = CornerRadius(r, r))
        return
    }
    val left = Offset(r, cy)
    val right = Offset(size.width - r, cy)
    drawCircle(color, r, left)
    drawCircle(color, r, right)
    // Neck half-height: full at the start, thinning to zero by p = 0.85.
    val t = (p / 0.85f).coerceIn(0f, 1f)
    val neck = r * (1f - t) * (1f - t)
    if (neck < 0.5f) return
    val mid = (left.x + right.x) / 2f
    // Quadratic edges that pass through (mid, cy ± neck).
    val ctrlTop = cy - 2f * neck + r
    val ctrlBottom = cy + 2f * neck - r
    val path = Path().apply {
        moveTo(left.x, cy - r)
        quadraticTo(mid, ctrlTop, right.x, cy - r)
        lineTo(right.x, cy + r)
        quadraticTo(mid, ctrlBottom, left.x, cy + r)
        close()
    }
    drawPath(path, color)
}
