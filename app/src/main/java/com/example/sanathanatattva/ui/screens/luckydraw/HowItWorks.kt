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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.sanathanatattva.data.model.DrawSchedule
import com.example.sanathanatattva.ui.components.cardBorder
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.util.formatRupees
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/** Qualifying months that earn one more entry, and the most entries a month can carry. */
const val TICKETS_PER_MONTHS = 3
const val MAX_TICKETS = 4

/** The languages the rules can be read in. [tag] tells TalkBack how to pronounce them. */
enum class RulesLanguage(val label: String, val tag: String) {
    English("English", "en-IN"),
    Kannada("ಕನ್ನಡ", "kn-IN"),
}

/** "12th", "3rd", "21st": the draw day, said the way a person says it. */
fun ordinalDay(day: Int): String {
    val rest = day % 100
    if (rest in 11..13) return "${day}th"
    return day.toString() + when (day % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
}

/** "17:00" reads as a timetable; "5pm" reads as a time of day. */
fun clockLabel(hhmm: String): String {
    val (hour, minute) = parseClock(hhmm) ?: return hhmm
    val suffix = if (hour < 12) "am" else "pm"
    return if (minute != 0) "${twelveHour(hour)}.${minute.toString().padStart(2, '0')}$suffix" else "${twelveHour(hour)}$suffix"
}

/**
 * The entry deadline as a Kannada speaker would say it, with the "by" built in:
 * "ತಿಂಗಳ 12ನೇ ತಾರೀಖು ಸಂಜೆ 5 ಗಂಟೆಯೊಳಗೆ" (by 5 in the evening on the 12th of the month).
 * Kannada names the part of the day rather than using am/pm.
 */
fun kannadaDeadline(schedule: DrawSchedule): String {
    val date = "ತಿಂಗಳ ${schedule.day}ನೇ ತಾರೀಖು"
    val (hour, minute) = parseClock(schedule.time) ?: return "$date ${schedule.time}ರೊಳಗೆ"
    val part = when (hour) {
        in 4..11 -> "ಬೆಳಿಗ್ಗೆ"   // morning
        in 12..15 -> "ಮಧ್ಯಾಹ್ನ" // afternoon
        in 16..19 -> "ಸಂಜೆ"     // evening
        else -> "ರಾತ್ರಿ"         // night
    }
    return if (minute == 0) "$date $part ${twelveHour(hour)} ಗಂಟೆಯೊಳಗೆ"
    else "$date $part ${twelveHour(hour)}:${minute.toString().padStart(2, '0')}ರೊಳಗೆ"
}

private fun parseClock(hhmm: String): Pair<Int, Int>? {
    val parts = hhmm.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    return hour to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

private fun twelveHour(hour: Int) = if (hour % 12 == 0) 12 else hour % 12

data class HowItWorksCopy(
    val title: String,
    val subtitle: String,
    val rules: List<AnnotatedString>,
    val ladderLabel: String,
)

/**
 * The seven rules, in the order someone meets them: pay, buy, wait, win. [joiningFee] and
 * [schedule] are null when unknown (signed out, or no round open); the copy then speaks
 * about them in general rather than inventing numbers. [strong] styles the phrases that
 * carry the deal.
 */
fun howItWorksCopy(
    language: RulesLanguage,
    joiningFee: Double?,
    schedule: DrawSchedule?,
    strong: SpanStyle,
): HowItWorksCopy = when (language) {
    RulesLanguage.English -> {
        val by = schedule?.let { "the ${ordinalDay(it.day)}, ${clockLabel(it.time)}" }
        val fee = joiningFee?.let { "the ${formatRupees(it)}" } ?: "the joining fee"
        HowItWorksCopy(
            title = "How it works",
            subtitle = "The whole thing, in seven lines.",
            rules = listOf(
                buildAnnotatedString {
                    append("The round runs for ")
                    withStyle(strong) { append("twelve months") }
                    append(". You pay $fee once, at the start, and never again.")
                },
                buildAnnotatedString {
                    append("Buy any oil before ")
                    withStyle(strong) { append(by ?: "each month's draw") }
                    append(" and you are in that month's draw. One bottle counts the same as five. Every month is a fresh chance.")
                },
                AnnotatedString("Nothing bought by ${by ?: "the draw"}, and you sit that month out. Buy after it and your " +
                    "name goes into the next month's draw instead, so a late order is never a wasted one."),
                AnnotatedString("Every $TICKETS_PER_MONTHS months you qualify, your name goes in one more time, up to " +
                    "$MAX_TICKETS. Buying steadily is worth more than buying big."),
                buildAnnotatedString {
                    append("Win, and ")
                    withStyle(strong) { append("the oil you bought that month comes to you free") }
                    append(", every month, for the rest of the round. Win in month three and that is ten free deliveries, this month included.")
                },
                AnnotatedString("Reach month twelve without winning and your last order is delivered free in month " +
                    "thirteen. Nobody finishes $LUCKY_DRAW_NAME with nothing."),
                AnnotatedString("One round at a time, and one winner each month. A winner steps out of the draw and keeps receiving."),
            ),
            ladderLabel = "month ${TICKETS_PER_MONTHS * (MAX_TICKETS - 1)}",
        )
    }
    RulesLanguage.Kannada -> {
        val by = schedule?.let { kannadaDeadline(it) } ?: "ಪ್ರತಿ ತಿಂಗಳ ಡ್ರಾಗೆ ಮುನ್ನ"
        val fee = joiningFee?.let { "${formatRupees(it)} ಅನ್ನು" } ?: "ಸೇರ್ಪಡೆ ಶುಲ್ಕವನ್ನು"
        HowItWorksCopy(
            title = "ಇದು ಹೇಗೆ ಕೆಲಸ ಮಾಡುತ್ತದೆ",
            subtitle = "ಎಲ್ಲವೂ, ಏಳು ಸಾಲುಗಳಲ್ಲಿ.",
            rules = listOf(
                buildAnnotatedString {
                    append("ಈ ಸುತ್ತು ")
                    withStyle(strong) { append("ಹನ್ನೆರಡು ತಿಂಗಳು") }
                    append(" ನಡೆಯುತ್ತದೆ. $fee ಆರಂಭದಲ್ಲಿ ಒಮ್ಮೆ ಮಾತ್ರ ಪಾವತಿಸುತ್ತೀರಿ, ಮತ್ತೆ ಎಂದಿಗೂ ಇಲ್ಲ.")
                },
                buildAnnotatedString {
                    withStyle(strong) { append(by) }
                    append(" ಯಾವುದೇ ಎಣ್ಣೆ ಖರೀದಿಸಿದರೆ, ಆ ತಿಂಗಳ ಡ್ರಾದಲ್ಲಿ ನಿಮ್ಮ ಹೆಸರು ಇರುತ್ತದೆ. ಒಂದು ಬಾಟಲಿಯೂ ಐದು " +
                        "ಬಾಟಲಿಗಳಷ್ಟೇ ಲೆಕ್ಕ. ಪ್ರತಿ ತಿಂಗಳೂ ಹೊಸ ಅವಕಾಶ.")
                },
                AnnotatedString("$by ಏನೂ ಖರೀದಿಸದಿದ್ದರೆ, ಆ ತಿಂಗಳ ಡ್ರಾದಲ್ಲಿ ನೀವು ಇರುವುದಿಲ್ಲ. ಅದರ ನಂತರ ಖರೀದಿಸಿದರೆ ನಿಮ್ಮ " +
                    "ಹೆಸರು ಮುಂದಿನ ತಿಂಗಳ ಡ್ರಾಗೆ ಸೇರುತ್ತದೆ, ಹಾಗಾಗಿ ತಡವಾದ ಆರ್ಡರ್ ಎಂದಿಗೂ ವ್ಯರ್ಥವಲ್ಲ."),
                AnnotatedString("ನೀವು ಅರ್ಹರಾಗುವ ಪ್ರತಿ $TICKETS_PER_MONTHS ತಿಂಗಳಿಗೊಮ್ಮೆ, ನಿಮ್ಮ ಹೆಸರು ಇನ್ನೊಂದು ಬಾರಿ " +
                    "ಡ್ರಾಗೆ ಸೇರುತ್ತದೆ, ಗರಿಷ್ಠ $MAX_TICKETS ಬಾರಿಯವರೆಗೆ. ದೊಡ್ಡದಾಗಿ ಖರೀದಿಸುವುದಕ್ಕಿಂತ ನಿಯಮಿತವಾಗಿ " +
                    "ಖರೀದಿಸುವುದು ಹೆಚ್ಚು ಲಾಭದಾಯಕ."),
                buildAnnotatedString {
                    append("ಗೆದ್ದರೆ, ")
                    withStyle(strong) { append("ಆ ತಿಂಗಳು ನೀವು ಖರೀದಿಸಿದ ಎಣ್ಣೆ ನಿಮಗೆ ಉಚಿತವಾಗಿ ಬರುತ್ತದೆ") }
                    append(", ಸುತ್ತಿನ ಉಳಿದ ಪ್ರತಿ ತಿಂಗಳೂ. ಮೂರನೇ ತಿಂಗಳಲ್ಲಿ ಗೆದ್ದರೆ, ಈ ತಿಂಗಳೂ ಸೇರಿ ಹತ್ತು ಉಚಿತ ಡೆಲಿವರಿಗಳು.")
                },
                AnnotatedString("ಗೆಲ್ಲದೆ ಹನ್ನೆರಡನೇ ತಿಂಗಳು ತಲುಪಿದರೆ, ನಿಮ್ಮ ಕೊನೆಯ ಆರ್ಡರ್ ಹದಿಮೂರನೇ ತಿಂಗಳಲ್ಲಿ ಉಚಿತವಾಗಿ " +
                    "ತಲುಪುತ್ತದೆ. ಭಾಗ್ಯ ಚಕ್ರದಿಂದ ಯಾರೂ ಬರಿಗೈಯಲ್ಲಿ ಹೋಗುವುದಿಲ್ಲ."),
                AnnotatedString("ಒಮ್ಮೆಗೆ ಒಂದೇ ಸುತ್ತು, ಪ್ರತಿ ತಿಂಗಳು ಒಬ್ಬರೇ ವಿಜೇತರು. ವಿಜೇತರು ಡ್ರಾದಿಂದ ಹೊರಗುಳಿದು, ಉಚಿತ " +
                    "ಡೆಲಿವರಿಗಳನ್ನು ಪಡೆಯುತ್ತಲೇ ಇರುತ್ತಾರೆ."),
            ),
            ladderLabel = "${TICKETS_PER_MONTHS * (MAX_TICKETS - 1)}ನೇ ತಿಂಗಳು",
        )
    }
}

private val RuleIcons: List<ImageVector?> = listOf(
    Icons.Outlined.CalendarMonth, Icons.Outlined.ShoppingBag, Icons.Outlined.EventBusy,
    Icons.Outlined.ConfirmationNumber, null, Icons.Outlined.Replay, Icons.Outlined.VerifiedUser,
)
/** The rule that gets the drawn ticket ladder under it. */
private const val LADDER_RULE = 3

/**
 * The rules card, readable in English or Kannada. It opens in Kannada when the phone is
 * set to Kannada; the switch is there for everyone else. Shown on the round page whatever
 * state the account is in.
 */
@Composable
internal fun HowItWorksCard(joiningFee: Double?, schedule: DrawSchedule?, modifier: Modifier = Modifier) {
    var language by rememberSaveable {
        mutableStateOf(if (Locale.getDefault().language == "kn") RulesLanguage.Kannada else RulesLanguage.English)
    }
    val strong = SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    val copy = howItWorksCopy(language, joiningFee, schedule, strong)
    val locale = TextStyle(localeList = LocaleList(language.tag))
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = cardBorder(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(copy.title, style = MaterialTheme.typography.titleMedium.merge(locale))
            Text(copy.subtitle, style = MaterialTheme.typography.bodySmall.merge(locale),
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            SingleChoiceSegmentedButtonRow(Modifier.padding(top = 12.dp, bottom = 16.dp)) {
                RulesLanguage.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = language == option,
                        onClick = { language = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = RulesLanguage.entries.size),
                    ) { Text(option.label, style = TextStyle(localeList = LocaleList(option.tag))) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                copy.rules.forEachIndexed { index, rule ->
                    Rule(RuleIcons[index], rule, locale) {
                        if (index == LADDER_RULE) TicketLadder(copy.ladderLabel)
                    }
                }
            }
        }
    }
}

/** One rule: its icon (the wheel itself when [icon] is null) beside the line. */
@Composable
private fun Rule(icon: ImageVector?, text: AnnotatedString, locale: TextStyle, extra: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.padding(top = 2.dp).size(20.dp), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            else ChakraWheel(Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge.merge(locale),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun TicketLadder(label: String) {
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
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
    }
}
