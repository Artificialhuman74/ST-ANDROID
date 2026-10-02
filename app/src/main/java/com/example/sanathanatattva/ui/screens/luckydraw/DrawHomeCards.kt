package com.example.sanathanatattva.ui.screens.luckydraw

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sanathanatattva.data.model.DrawCycle
import com.example.sanathanatattva.data.model.DrawResult
import com.example.sanathanatattva.data.model.DrawReward
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.ui.theme.EmblemSerif
import com.example.sanathanatattva.ui.theme.LogoCream
import com.example.sanathanatattva.ui.theme.OilGold
import com.example.sanathanatattva.ui.theme.OilLight
import com.example.sanathanatattva.ui.theme.PressGreen
import kotlinx.coroutines.launch

/** The draw's own accent: the gold of the wheel's grain, for ticks and the round rail. */
internal val DrawAccent = Color(0xFFC8963C)
private val DrawShape = RoundedCornerShape(16.dp)
/** Ease-out-expo: the gold pass, the tick and the pour all decelerate onto their mark. */
private val ArriveEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * The home page's Bhagya Chakra card: one of the states in [DrawHomeCard], swapped with a
 * soft cross-fade when a draw changes which one applies. Anyone not yet in a round,
 * signed in or not, sees the promotional banner.
 */
@Composable
fun LuckyDrawShopCard(navController: NavController, session: SessionViewModel) {
    val loggedIn by session.isLoggedIn.collectAsState()
    val consumer by session.consumer.collectAsState()
    if (loggedIn == null) return // the session is still being read; nothing to promise yet
    val vm = consumer?.id?.takeIf { loggedIn == true }?.let { drawViewModel(it) }
    val state = vm?.state?.collectAsState()?.value
    if (vm != null) RefreshDrawOnResume(vm)
    val card = when {
        loggedIn == false -> DrawHomeCard.Promo(signedIn = false)
        state == null -> DrawHomeCard.Promo(signedIn = true)
        else -> homeCardFor(state.status, state.unseenResult, state.resultDismissed)
    }
    val openRound = { navController.navigate(Routes.LUCKY_DRAW) }
    AnimatedContent(
        targetState = card,
        contentKey = { it::class },
        transitionSpec = {
            (fadeIn(tween(320, delayMillis = 90)) + scaleIn(tween(320, delayMillis = 90, easing = ArriveEasing), 0.97f))
                .togetherWith(fadeOut(tween(140)))
                .using(SizeTransform(clip = false))
        },
        label = "Bhagya Chakra card",
    ) { shown ->
        when (shown) {
            is DrawHomeCard.Promo -> PromoBanner(
                promo = shown,
                onOpen = openRound,
                onAction = if (shown.signedIn) openRound else ({ navController.navigate(Routes.LOGIN) }),
            )
            is DrawHomeCard.ResultWaiting -> ResultWaitingCard(shown.cycle, onSee = openRound)
            is DrawHomeCard.Winner -> WinnerCard(
                cycle = shown.cycle,
                reward = shown.reward,
                claiming = state?.busy == CLAIM_BUSY,
                enabled = state?.busy == null,
                justClaimed = state?.justClaimed == shown.cycle.month_key,
                error = state?.claimError,
                onClaim = { vm?.claimReward() },
                onHowItWorks = openRound,
            )
            is DrawHomeCard.Result -> ResultCard(shown.cycle, shown.draw,
                onShop = { vm?.dismissResult() }, onDismiss = { vm?.dismissResult() })
            is DrawHomeCard.Enrolled -> EnrolledRow(shown.cycle,
                firstLook = { vm?.firstLookAtEntry(shown.cycle) == true }, onOpen = openRound)
            is DrawHomeCard.Joined -> JoinedRow(shown.schemeName, shown.won, onOpen = openRound)
        }
    }
}

@Composable
private fun DrawSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val border = BorderStroke(1.dp, colors.tertiary.copy(alpha = 0.22f))
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = DrawShape,
            color = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer, border = border, content = content)
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = DrawShape,
            color = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer, border = border, content = content)
    }
}

/** The small wheel + label line that heads the larger cards. */
@Composable
private fun DrawEyebrow(text: String, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChakraWheel(Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary)
        trailing()
    }
}

@Composable
private fun bodyColor() = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.86f)

// ── Not in a round yet: the promotion ────────────────────────────────────

private val PromoDeep = Color(0xFF072318)
private val PromoInk = Color(0xFF2A1E05)

/**
 * The shop's promotion for the draw, in the brand-moment palette rather than the quiet
 * card style: press green, with the cart wheel turning at a ghani stone's pace in a soft
 * gold glow. The wheel is the only thing that moves. Tapping anywhere opens the round;
 * the button does the next real thing.
 */
@Composable
internal fun PromoBanner(promo: DrawHomeCard.Promo, onOpen: () -> Unit, onAction: () -> Unit) {
    val copy = promoCopy(promo)
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = DrawShape,
        color = PressGreen,
        contentColor = LogoCream,
    ) {
        Box(
            Modifier
                .heightIn(min = 188.dp)
                .drawBehind {
                    drawRect(Brush.radialGradient(
                        0f to PressGreen, 1f to PromoDeep,
                        center = Offset(size.width * 0.82f, size.height * 0.5f), radius = size.width * 0.9f,
                    ))
                },
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp)
                    .size(180.dp)
                    .drawWithCache {
                        val glow = Brush.radialGradient(
                            0f to OilGold.copy(alpha = 0.28f), 0.62f to OilGold.copy(alpha = 0.05f),
                            1f to Color.Transparent, radius = size.minDimension / 2f,
                        )
                        onDrawBehind { drawCircle(glow) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                ChakraWheel(Modifier.size(104.dp), WheelMotion.Wait)
            }
            Column(Modifier.fillMaxWidth(0.6f).padding(start = 20.dp, top = 20.dp, bottom = 20.dp)) {
                Text(LUCKY_DRAW_NAME.uppercase(), style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.18.em, color = OilLight)
                Spacer(Modifier.height(8.dp))
                Text("Win your oil free, every month", fontFamily = EmblemSerif, fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp, lineHeight = 27.sp, color = LogoCream)
                Spacer(Modifier.height(8.dp))
                Text(copy.body, style = MaterialTheme.typography.bodyMedium, color = LogoCream.copy(alpha = 0.8f))
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = OilGold, contentColor = PromoInk),
                ) { Text(copy.action, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ── Enrolled ─────────────────────────────────────────────────────────────

/**
 * The standing line all month. The delight is the crossing: the first time this row shows
 * someone as in this month's draw, one gold pass runs over it and the tick draws in.
 */
@Composable
internal fun EnrolledRow(cycle: DrawCycle, firstLook: suspend () -> Boolean, onOpen: () -> Unit) {
    val line = enrolledLine(cycle)
    val reduced = rememberReducedMotion()
    val sweep = remember { Animatable(-1f) }
    val tick = remember { Animatable(1f) }
    LaunchedEffect(cycle.scheme_id, cycle.month_key, line.inThisDraw) {
        if (line.inThisDraw && firstLook() && !reduced) {
            tick.snapTo(0f)
            launch { tick.animateTo(1f, tween(420, delayMillis = 380, easing = ArriveEasing)) }
            sweep.snapTo(-1f)
            sweep.animateTo(1f, tween(1100, easing = ArriveEasing))
        }
    }
    DrawSurface(
        onClick = onOpen,
        modifier = Modifier
            .clip(DrawShape)
            .drawWithContent {
                drawContent()
                val at = sweep.value
                if (at > -1f && at < 1f) translate(left = at * size.width) {
                    drawRect(Brush.horizontalGradient(
                        0f to Color(0x00F0D898), 0.5f to Color(0x8CF0D898), 1f to Color(0x00F0D898),
                    ))
                }
            },
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChakraWheel(Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("You're in ${cycle.scheme_name}", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (line.inThisDraw) TickBadge(Modifier.graphicsLayer {
                        scaleX = tick.value
                        scaleY = tick.value
                    })
                    Text(line.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DrawAccent)
        }
    }
}

@Composable
private fun TickBadge(modifier: Modifier = Modifier, size: Int = 16, color: Color = DrawAccent) {
    Box(modifier.size(size.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFFFDF8EE), modifier = Modifier.size((size * 0.7f).dp))
    }
}

@Composable
internal fun JoinedRow(schemeName: String, won: Boolean, onOpen: () -> Unit) {
    DrawSurface(onClick = onOpen) {
        Row(
            Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChakraWheel(Modifier.size(24.dp))
            Text(if (won) "You've won this round of $schemeName" else "You're in $schemeName",
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DrawAccent)
        }
    }
}

// ── The draw has run ─────────────────────────────────────────────────────

/**
 * Everyone in the round, winner or not, before they've watched the reveal. It names
 * nobody and can't be dismissed. The wheel turns at a ghani stone's pace: a held breath.
 */
@Composable
internal fun ResultWaitingCard(cycle: DrawCycle, onSee: () -> Unit) {
    val reduced = rememberReducedMotion()
    val breath = if (reduced) 0.5f else {
        val t = rememberInfiniteTransition(label = "Waiting halo")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "Breath").value
    }
    DrawSurface {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.55f + 0.35f * breath
                            scaleX = 1f + 0.1f * breath
                            scaleY = scaleX
                        }
                        .drawWithCache {
                            val halo = Brush.radialGradient(
                                0f to DrawAccent.copy(alpha = 0.35f), 0.55f to DrawAccent.copy(alpha = 0.08f),
                                0.72f to Color.Transparent, radius = size.minDimension / 2f,
                            )
                            onDrawBehind { drawCircle(halo) }
                        },
                )
                ChakraWheel(Modifier.size(48.dp), WheelMotion.Wait)
            }
            Spacer(Modifier.height(16.dp))
            Text("${cycle.scheme_name} has been drawn.", textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(6.dp))
            Text("This month's result is waiting for you.", textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge, color = bodyColor())
            Button(onClick = onSee, modifier = Modifier.padding(top = 24.dp).heightIn(min = 48.dp)) {
                Text("See the result", modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

/** After watching, for those who didn't win: who did, and what puts them in the next draw. */
@Composable
internal fun ResultCard(cycle: DrawCycle, draw: DrawResult, onShop: () -> Unit, onDismiss: () -> Unit) {
    val nextDraw = !cycle.round_over && cycle.month_number < cycle.total_months
    DrawSurface {
        Column(Modifier.padding(20.dp)) {
            DrawEyebrow("This month's draw")
            Spacer(Modifier.height(12.dp))
            Text(resultHeadline(cycle, draw),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text(resultBody(cycle, draw), style = MaterialTheme.typography.bodyLarge, color = bodyColor())
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (nextDraw) Button(onClick = onShop) { Text("Shop now to enter") }
                TextButton(onClick = onDismiss) { Text("Got it", color = MaterialTheme.colorScheme.tertiary) }
            }
        }
    }
}

// ── Won ──────────────────────────────────────────────────────────────────

/**
 * The winner's card, every month of the round that's left. Winning hands nothing over on
 * its own: each month's reward is a ₹0 order they place, so this is an action card, not
 * a trophy. The rail at its foot is the part that changes month to month.
 */
@Composable
internal fun WinnerCard(
    cycle: DrawCycle,
    reward: DrawReward,
    claiming: Boolean,
    enabled: Boolean,
    justClaimed: Boolean,
    error: String?,
    onClaim: () -> Unit,
    onHowItWorks: (() -> Unit)?,
) {
    // Optimistic: the rail fills the moment the claim lands; the refresh makes it true.
    val claimed = reward.this_month_ordered || justClaimed
    val delivered = reward.this_month_delivered
    val colors = MaterialTheme.colorScheme
    DrawSurface {
        Column(Modifier.padding(20.dp)) {
            DrawEyebrow(winnerLabel(cycle, reward, claimed))
            Spacer(Modifier.height(12.dp))
            Text(
                buildAnnotatedString {
                    append(reward.product_name ?: "Your reward delivery")
                    if (reward.product_name != null && reward.quantity > 1) {
                        withStyle(SpanStyle(color = colors.tertiary, fontWeight = FontWeight.SemiBold)) {
                            append(" × ${reward.quantity}")
                        }
                    }
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(6.dp))
            Text("Free, because you won ${cycle.scheme_name}.", style = MaterialTheme.typography.bodyLarge, color = bodyColor())
            Spacer(Modifier.height(20.dp))
            when {
                cycle.round_over && !claimed -> Text(roundCompleteLine(cycle, reward),
                    style = MaterialTheme.typography.titleSmall, color = bodyColor())
                claimed -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (delivered) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Inventory2, null, tint = colors.onPrimary, modifier = Modifier.size(13.dp))
                        }
                    } else TickBadge(size = 22)
                    Text(winnerConfirmation(cycle, reward), style = MaterialTheme.typography.titleMedium,
                        color = if (delivered) colors.primary else colors.tertiary)
                }
                else -> Button(
                    onClick = { if (!claiming) onClaim() },
                    enabled = enabled || claiming,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    if (claiming) {
                        MiniWheelSpinner()
                        Spacer(Modifier.width(10.dp))
                        Text("Placing your order")
                    } else Text("Place my free order · ₹0")
                }
            }
            if (error != null && !claimed) {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(error, style = MaterialTheme.typography.bodyMedium, color = colors.error, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClaim, enabled = enabled) { Text("Try again") }
                }
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = DrawAccent.copy(alpha = 0.28f))
            Spacer(Modifier.height(16.dp))
            RoundRail(current = cycle.month_number, total = cycle.total_months, claimed = claimed,
                pour = justClaimed, pourKey = cycle.month_key)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Month ${cycle.month_number} of ${cycle.total_months}", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold, color = colors.tertiary, modifier = Modifier.weight(1f))
                if (onHowItWorks != null) {
                    TextButton(onClick = onHowItWorks) { Text("How $LUCKY_DRAW_NAME works", color = colors.tertiary) }
                }
            }
        }
    }
}

/**
 * The round as a rail of its months: past months full, this month standing proud of the
 * line and breathing while there's something to claim, the rest empty track. Claiming
 * pours this month's segment full, once.
 */
@Composable
internal fun RoundRail(current: Int, total: Int, claimed: Boolean, pour: Boolean, pourKey: String) {
    val span = total.coerceIn(1, 24)
    val at = current.coerceIn(1, span)
    val reduced = rememberReducedMotion()
    var poured by rememberSaveable(pourKey) { mutableStateOf(false) }
    val fill = remember { Animatable(1f) }
    LaunchedEffect(pour, pourKey) {
        if (pour && !poured) {
            poured = true
            if (!reduced) {
                fill.snapTo(0f)
                fill.animateTo(1f, tween(720, easing = ArriveEasing))
            }
        }
    }
    val live = !claimed && !reduced
    val breath = if (live) {
        val t = rememberInfiniteTransition(label = "Month live")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200)), label = "Ring").value
    } else 0f
    Row(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
            .semantics { contentDescription = "Month $at of $span. ${if (claimed) "Claimed." else "Not claimed yet."}" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (month in 1..span) {
            val isCurrent = month == at
            val past = month < at
            val filled = past || (isCurrent && claimed)
            Box(
                Modifier
                    .weight(1f)
                    .height(if (isCurrent) 10.dp else 6.dp)
                    .drawBehind {
                        val radius = CornerRadius(size.height / 2f)
                        if (!filled) {
                            drawRoundRect(DrawAccent.copy(alpha = 0.15f), cornerRadius = radius)
                            if (isCurrent) {
                                drawRoundRect(DrawAccent.copy(alpha = 0.5f), cornerRadius = radius, style = Stroke(1.5.dp.toPx()))
                                if (live) {
                                    val grow = 3.5.dp.toPx() * breath
                                    drawRoundRect(
                                        DrawAccent.copy(alpha = 0.45f * (1f - breath)),
                                        topLeft = Offset(-grow, -grow),
                                        size = Size(size.width + grow * 2, size.height + grow * 2),
                                        cornerRadius = CornerRadius(size.height / 2f + grow),
                                        style = Stroke(1.5.dp.toPx()),
                                    )
                                }
                            }
                        } else {
                            val brush = if (past) Brush.horizontalGradient(listOf(Color(0xFFE8B86D), Color(0xFFA87830)))
                            else Brush.horizontalGradient(0f to Color(0xFFF0D898), 0.55f to DrawAccent, 1f to Color(0xFFA87830))
                            val width = if (isCurrent) size.width * fill.value else size.width
                            drawRoundRect(DrawAccent.copy(alpha = 0.15f), cornerRadius = radius)
                            drawRoundRect(brush, size = Size(width, size.height), cornerRadius = radius)
                        }
                    },
            )
        }
    }
}
