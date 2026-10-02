package com.example.sanathanatattva.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sanathanatattva.ui.theme.OilGold
import com.example.sanathanatattva.ui.theme.PressGreen

/**
 * The order seal at rest: a press-green disc with an oil-gold rim and check.
 * It is exactly where the order celebration settles, so the overlay can hand
 * off to it without a seam. (Gold here marks a completed payment.) The rim
 * draws [SEAL_RIM_DP] outside [diameter], so layout matches a plain icon.
 */
@Composable
fun OrderSealMedallion(modifier: Modifier = Modifier, diameter: Dp = 72.dp) {
    Canvas(modifier.size(diameter)) {
        val r = diameter.toPx() / 2f
        drawCircle(OilGold, r + SEAL_RIM_DP.dp.toPx())
        drawCircle(PressGreen, r)
        drawSealCheck(
            center = center,
            scale = r / EMBLEM_RING_DP.dp.toPx(),
            progress = 1f,
            color = OilGold,
        )
    }
}
