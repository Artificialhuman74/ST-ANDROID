package com.example.sanathanatattva.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/** One destination in the [LiquidDock]. */
data class DockItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * Space the dock occupies above the system navigation bar (its 64dp body plus the
 * 8dp margins). Screens whose content scrolls beneath the floating dock add this to
 * their bottom padding so nothing is trapped underneath it.
 */
val DockContentClearance = 88.dp

/**
 * A floating bottom navigation dock. The "liquid" is the motion, not the material:
 * a single brand-green selection pill glides between destinations on a spring and
 * stretches slightly along the direction of travel, so switching tabs reads as one
 * continuous morph rather than four separate on/off states.
 *
 * Legibility is never traded away for the effect — the dock is fully opaque and
 * icons/labels sit at full contrast (deliberately not glassmorphic, per DESIGN.md).
 * Under system reduced-motion the pill snaps instead of gliding.
 */
@Composable
fun LiquidDock(
    items: List<DockItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val reduced = rememberReducedMotion()
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)

    // Continuous position of the pill in "slot units" (0..lastIndex). The whole
    // effect hangs off this one animated value.
    val animIndex by animateFloatAsState(
        targetValue = safeIndex.toFloat(),
        animationSpec = if (reduced) {
            snap()
        } else {
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "dockPill",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 8.dp,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(6.dp),
        ) {
            val count = items.size
            val trackWidthPx = constraints.maxWidth.toFloat()
            val slotPx = trackWidthPx / count
            val density = LocalDensity.current
            val pillInsetPx = with(density) { 6.dp.toPx() }
            val pillWidthDp = with(density) { (slotPx - pillInsetPx * 2f).toDp() }

            // Fractional distance from the nearest slot → 0 at rest, ~0.5 mid-flight.
            val frac = animIndex - animIndex.roundToInt()
            val stretch = 1f + 0.30f * (abs(frac) * 2f)

            Box(
                Modifier
                    .offset {
                        IntOffset((slotPx * animIndex + pillInsetPx).roundToInt(), 0)
                    }
                    .width(pillWidthDp)
                    .fillMaxHeight()
                    .graphicsLayer { scaleX = stretch }
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(22.dp),
                    ),
            )

            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, item ->
                    // 1 when the pill is centred on this item, fading to 0 as it leaves.
                    val on = (1f - abs(animIndex - i)).coerceIn(0f, 1f)
                    val contentColor = lerp(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.onPrimary,
                        on,
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .selectable(
                                selected = i == safeIndex,
                                role = Role.Tab,
                                onClick = { onSelect(i) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (on > 0.5f) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    val s = 1f + 0.08f * on
                                    scaleX = s
                                    scaleY = s
                                },
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
