package com.example.sanathanatattva.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────
// Cards
// ─────────────────────────────────────────────────────────────────────

/**
 * THE knob for the whole app's card look. Every card's outline comes from
 * [cardBorder], which reads this. To iterate the card treatment, change this
 * one value:
 *   - 0.dp  → borderless (cards separate by fill/elevation only)
 *   - 1.dp  → current hairline outline
 *   - 2.dp+ → a bolder, more defined edge
 */
val CardOutlineWidth: Dp = 1.dp

/**
 * The single, shared card outline. Cards sit on a near-white background and are
 * defined by this hairline (a green-tinted `outlineVariant`) rather than by any
 * fill trick — one definition so the whole app's cards stay consistent and are
 * tuned from one place ([CardOutlineWidth]). Works in light and dark, since the
 * colour is a theme token.
 */
@Composable
fun cardBorder(): BorderStroke =
    BorderStroke(CardOutlineWidth, MaterialTheme.colorScheme.outlineVariant)

/**
 * A leading-icon "medallion": an icon on a soft rounded tonal tile. This is how
 * the app colorises list rows, section leads and states — the tint carries a
 * little warmth and (where chosen deliberately) meaning, while the icon keeps
 * the signal legible. Colour is never the only signal; a medallion always sits
 * next to a text label.
 *
 * Defaults to the calm sage container so callers can opt into brand green or
 * (money only) gold where it means something.
 */
@Composable
fun TintedIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    corner: Dp = size / 2,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(iconSize),
        )
    }
}
