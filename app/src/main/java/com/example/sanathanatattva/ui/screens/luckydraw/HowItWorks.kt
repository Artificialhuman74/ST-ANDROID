package com.example.sanathanatattva.ui.screens.luckydraw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.sanathanatattva.data.model.DrawSchedule
import com.example.sanathanatattva.ui.components.cardBorder
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.util.formatRupees
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Qualifying months that earn one more entry, and the most entries a month can carry. */
const val TICKETS_PER_MONTHS = 3
const val MAX_TICKETS = 4

/** "12th", "3rd", "21st": the draw day, said the way a person says it. */
fun ordinalDay(day: Int): String {
    val rest = day % 100
    if (rest in 11..13) return "${day}th"
    return day.toString() + when (day % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
}

/** "17:00" reads as a timetable; "5pm" reads as a time of day. */
fun clockLabel(hhmm: String): String {
    val parts = hhmm.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val suffix = if (hour < 12) "am" else "pm"
    val twelve = if (hour % 12 == 0) 12 else hour % 12
    return if (minute != 0) "$twelve.${minute.toString().padStart(2, '0')}$suffix" else "$twelve$suffix"
}

/**
 * The rules, in the order someone meets them: pay, buy, wait, win. This is the card that
 * decides whether the joining fee feels like a fair deal, so every line names a real
 * thing that will happen, with the draw date the admin has actually set. Shown on the
 * round page whatever state the account is in.
 *
 * [joiningFee] and [schedule] are null when they aren't known (signed out, or no round
 * open); the copy then speaks about them in general rather than inventing numbers.
 */
@Composable
internal fun HowItWorksCard(joiningFee: Double?, schedule: DrawSchedule?, modifier: Modifier = Modifier) {
    val strong = SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    val by = schedule?.let { "the ${ordinalDay(it.day)}, ${clockLabel(it.time)}" }
    val fee = joiningFee?.let { "the ${formatRupees(it)}" } ?: "the joining fee"
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = cardBorder(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("How it works", style = MaterialTheme.typography.titleMedium)
            Text("The whole thing, in seven lines.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Rule(Icons.Outlined.CalendarMonth, buildAnnotatedString {
                    append("The round runs for ")
                    withStyle(strong) { append("twelve months") }
                    append(". You pay $fee once, at the start, and never again.")
                })
                Rule(Icons.Outlined.ShoppingBag, buildAnnotatedString {
                    append("Buy any oil before ")
                    withStyle(strong) { append(by ?: "each month's draw") }
                    append(" and you are in that month's draw. One bottle counts the same as five. Every month is a fresh chance.")
                })
                Rule(Icons.Outlined.EventBusy, AnnotatedString(
                    "Nothing bought by ${by ?: "the draw"}, and you sit that month out. Buy after it and your name goes " +
                        "into the next month's draw instead, so a late order is never a wasted one.",
                ))
                Rule(Icons.Outlined.ConfirmationNumber, AnnotatedString(
                    "Every $TICKETS_PER_MONTHS months you qualify, your name goes in one more time, up to $MAX_TICKETS. " +
                        "Buying steadily is worth more than buying big.",
                )) { TicketLadder() }
                Rule(null, buildAnnotatedString {
                    append("Win, and ")
                    withStyle(strong) { append("the oil you bought that month comes to you free") }
                    append(", every month, for the rest of the round. Win in month three and that is ten free deliveries, this month included.")
                })
                Rule(Icons.Outlined.Replay, AnnotatedString(
                    "Reach month twelve without winning and your last order is delivered free in month thirteen. " +
                        "Nobody finishes $LUCKY_DRAW_NAME with nothing.",
                ))
                Rule(Icons.Outlined.VerifiedUser, AnnotatedString(
                    "One round at a time, and one winner each month. A winner steps out of the draw and keeps receiving.",
                ))
            }
        }
    }
}

/** One rule: its icon (the wheel itself when [icon] is null) beside the line. */
@Composable
private fun Rule(icon: ImageVector?, text: AnnotatedString, extra: @Composable () -> Unit = {}) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.padding(top = 2.dp).size(20.dp), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            else ChakraWheel(Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            extra()
        }
    }
}

private val LadderEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * The loyalty rule, drawn: four rungs grow out from the left, one after another. It's the
 * one rule people can't picture from a sentence. The top rate arrives at the START of the
 * top tier (entries are 1 + months / 3), so the label says month nine, not twelve.
 */
@Composable
private fun TicketLadder() {
    val reduced = rememberReducedMotion()
    val rungs = remember { List(MAX_TICKETS) { Animatable(if (reduced) 1f else 0f) } }
    LaunchedEffect(Unit) {
        rungs.forEachIndexed { i, rung ->
            launch {
                delay(i * 110L)
                rung.animateTo(1f, tween(550, easing = LadderEasing))
            }
        }
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        rungs.forEachIndexed { i, rung ->
            Box(
                Modifier
                    .width((26 + i * 9).dp)
                    .height(6.dp)
                    .graphicsLayer {
                        scaleX = rung.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        alpha = 1f - i * 0.16f
                    }
                    .background(DrawAccent, RoundedCornerShape(50)),
            )
        }
        Text("month ${TICKETS_PER_MONTHS * (MAX_TICKETS - 1)}", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary)
    }
}
