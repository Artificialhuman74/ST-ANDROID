package com.example.sanathanatattva.ui.screens.luckydraw

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sanathanatattva.data.model.*
import com.example.sanathanatattva.ui.components.ErrorState
import com.example.sanathanatattva.ui.components.ReadableWidth
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.navigation.openTab
import com.example.sanathanatattva.ui.screens.orders.statusLabel
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.util.formatRupees
import com.razorpay.Checkout
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuckyDrawScreen(navController: NavController, session: SessionViewModel) {
    val loggedIn by session.isLoggedIn.collectAsState()
    val consumer by session.consumer.collectAsState()
    LaunchedEffect(loggedIn) { if (loggedIn == true) session.refreshProfile() }
    val id = consumer?.id
    if (loggedIn == true && id != null) {
        DrawContent(navController, session, drawViewModel(id))
    } else {
        Scaffold(topBar = {
            TopAppBar(title = { Text(LUCKY_DRAW_NAME) }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            })
        }) { padding ->
            if (loggedIn == false) Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) { ChakraWheel(Modifier.size(HeroWheel)) }
                Text("Join the monthly lucky draw", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text("Log in to join a round, see your entries and claim rewards.", textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { navController.navigate(Routes.LOGIN) }) { Text("Log in") }
                HowItWorksCard(joiningFee = null, schedule = null)
            } else Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                WagonWheelLoader(120.dp, WheelMotion.Spin)
            }
        }
    }
}

/**
 * How the round page arrives. The wheel spins while the round loads, brakes onto a spoke
 * when it lands, then flies to the hero mark while the page rises in under it. A fast
 * load (or animations off) skips straight to [Arrived]: a deceleration nobody saw start
 * would just be latency dressed up as polish.
 */
private enum class Arrival { Loading, Settling, Flying, Arrived }

private val FlightEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val HeroWheel = 44.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawContent(navController: NavController, session: SessionViewModel, vm: LuckyDrawViewModel) {
    val state by vm.state.collectAsState()
    val sessionConsumer by session.consumer.collectAsState()
    val context = LocalContext.current
    val busy = state.busy != null
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    RefreshDrawOnResume(vm, details = true)
    BackHandler(enabled = busy) { /* Keep the payment owner alive until its callback arrives. */ }
    LaunchedEffect(state.consumer?.linked_dealer_id) {
        if (state.consumer?.linked_dealer_id != null) session.refreshProfile()
    }
    LaunchedEffect(vm) {
        vm.payments.collect { payment ->
            val activity = context.findActivity()
            if (activity == null) { vm.paymentDismissed(); return@collect }
            val consumer = vm.state.value.consumer
            runCatching {
                Checkout().apply { setKeyID(payment.key) }.open(activity, JSONObject().apply {
                    put("name", "Sanathana Tattva")
                    put("description", "$LUCKY_DRAW_NAME enrollment")
                    put("order_id", payment.orderId)
                    put("amount", payment.amount)
                    put("currency", payment.currency)
                    put("theme", JSONObject().put("color", "#c8963c"))
                    put("prefill", JSONObject().apply {
                        put("name", consumer?.name.orEmpty())
                        put("email", consumer?.email.orEmpty())
                        put("contact", consumer?.phone.orEmpty())
                    })
                })
            }.onFailure { vm.paymentDismissed() }
        }
    }

    // ── The wheel's arrival ────────────────────────────────────────────────
    val reduced = rememberReducedMotion()
    val shownAt = remember { SystemClock.uptimeMillis() }
    var arrival by rememberSaveable {
        mutableStateOf(if (vm.state.value.status != null) Arrival.Arrived else Arrival.Loading)
    }
    val loaded = state.status != null
    LaunchedEffect(loaded) {
        if (loaded && arrival == Arrival.Loading) {
            arrival = if (reduced || SystemClock.uptimeMillis() - shownAt < 400) Arrival.Arrived else Arrival.Settling
        }
    }
    var rootOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var loaderRect by remember { mutableStateOf<Rect?>(null) }
    var heroRect by remember { mutableStateOf<Rect?>(null) }
    val flight = remember { Animatable(0f) }
    LaunchedEffect(arrival) {
        if (arrival == Arrival.Flying) {
            // The page has to lay out once before the wheel knows where it's going.
            snapshotFlow { heroRect }.first { it != null }
            if (loaderRect == null) loaderRect = heroRect
            flight.animateTo(1f, tween(650, easing = FlightEasing))
            arrival = Arrival.Arrived
        }
    }
    val labelAlpha by animateFloatAsState(if (arrival == Arrival.Loading) 1f else 0f, tween(500), label = "Loading label")
    val pageIn = when (arrival) {
        Arrival.Arrived -> 1f
        Arrival.Flying -> flight.value
        else -> 0f
    }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize().onGloballyPositioned { rootOffset = it.positionInRoot() }) {
        // Wide windows centre the page; the wheel's flight and the win moment below
        // stay on the full-screen box, so the reveal remains full-bleed.
        ReadableWidth {
        Scaffold(topBar = {
            TopAppBar(title = { Text(LUCKY_DRAW_NAME) }, navigationIcon = {
                IconButton(enabled = !busy, onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }, actions = {
                IconButton(enabled = !busy && !state.loading, onClick = { vm.refresh(true) }) {
                    Icon(Icons.Default.Refresh, "Refresh draw")
                }
            })
        }) { padding ->
            when {
                state.status == null && !state.loading -> ErrorState(
                    message = state.error ?: "Couldn't load the draw.", onRetry = { vm.refresh(true) },
                    modifier = Modifier.padding(padding),
                )
                arrival == Arrival.Loading || arrival == Arrival.Settling -> Box(
                    Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        WagonWheelLoader(
                            size = 120.dp,
                            motion = if (arrival == Arrival.Settling) WheelMotion.Settle else WheelMotion.Spin,
                            wheelModifier = Modifier.onGloballyPositioned { loaderRect = it.boundsInRoot() },
                            onSettled = { arrival = Arrival.Flying },
                        )
                        Text("Loading $LUCKY_DRAW_NAME…", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp).graphicsLayer { alpha = labelAlpha })
                    }
                }
                else -> RoundPage(
                    navController = navController,
                    state = state,
                    vm = vm,
                    busy = busy,
                    onLeave = { confirmLeave = true },
                    heroVisible = arrival == Arrival.Arrived,
                    onHeroPlaced = { heroRect = it },
                    modifier = Modifier
                        .padding(padding)
                        .graphicsLayer {
                            alpha = pageIn
                            translationY = (1f - pageIn) * 16.dp.toPx()
                        },
                )
            }
        }
        }

        // The one wheel in flight between the loader and the hero mark.
        val from = loaderRect
        val to = heroRect
        if (arrival == Arrival.Flying && from != null && to != null) {
            val at = lerp(from, to, flight.value)
            ChakraWheel(
                Modifier
                    .offset { IntOffset((at.left - rootOffset.x).roundToInt(), (at.top - rootOffset.y).roundToInt()) }
                    .size(with(density) { at.width.toDp() }),
            )
        }

        // The draw reveal, once per draw, over everything.
        val cycle = state.status?.cycle
        val draw = cycle?.latest_draw
        AnimatedVisibility(
            visible = arrival == Arrival.Arrived && state.unseenResult && draw != null,
            enter = EnterTransition.None,
            exit = fadeOut(tween(400)),
        ) {
            if (cycle == null || draw == null) return@AnimatedVisibility
            val crowned = draw.is_me && draw.has_winner
            val hasNextDraw = !cycle.round_over && cycle.month_number < cycle.total_months
            WinMoment(
                winnerName = if (draw.is_me) (sessionConsumer?.name ?: state.consumer?.name)?.takeIf { it.isNotBlank() } ?: "You"
                    else draw.winner_display_name ?: "A member of your round",
                isMe = draw.is_me,
                hasWinner = draw.has_winner,
                schemeName = cycle.scheme_name,
                rewardProductName = cycle.reward?.product_name ?: draw.reward_product_name,
                nextEntryDeadline = cycle.next_entry_deadline,
                hasNextDraw = hasNextDraw,
                onContinue = {
                    vm.acknowledgeResult()
                    // The winner stays for their claim card; everyone else is pointed at the next draw.
                    if (!crowned && hasNextDraw) navController.openTab(Routes.SHOP)
                },
                onBack = {
                    vm.acknowledgeResult()
                    navController.popBackStack()
                },
            )
        }
    }

    if (confirmLeave) AlertDialog(
        onDismissRequest = { if (!busy) confirmLeave = false },
        title = { Text("Leave ${state.status?.enrollment?.scheme_name ?: "this round"}?") },
        text = { Text("You will stop receiving draw entries and future free deliveries. Your trader will collect any reward " +
            "container eligible for return. You cannot rejoin this same round. This does not request a joining-fee refund.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { confirmLeave = false; vm.leave() }) { Text("Leave round") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Stay in round") } },
    )
}

@Composable
private fun RoundPage(
    navController: NavController,
    state: LuckyDrawUiState,
    vm: LuckyDrawViewModel,
    busy: Boolean,
    onLeave: () -> Unit,
    heroVisible: Boolean,
    onHeroPlaced: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = state.status ?: return
    val enrollment = status.enrollment
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                ChakraWheel(
                    Modifier
                        .size(HeroWheel)
                        .onGloballyPositioned { onHeroPlaced(it.boundsInRoot()) }
                        .graphicsLayer { alpha = if (heroVisible) 1f else 0f },
                    motion = if (busy) WheelMotion.Spin else WheelMotion.Still,
                )
            }
            Text(
                when {
                    enrollment?.status == "won" -> "You've won this round."
                    enrollment != null -> "You're in ${enrollment.scheme_name}."
                    else -> "A little luck. A lot to look forward to."
                },
                style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
            )
            Text("Sanathana Tattva's monthly lucky draw", style = MaterialTheme.typography.bodyMedium)
        }
        state.message?.let { DrawCard("Your round") { Text(it) } }
        state.error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(it)
                    TextButton(enabled = !busy, onClick = { vm.refresh(true) }) { Text("Refresh status") }
                }
            }
        }
        if (state.verificationPending) {
            Button(onClick = vm::verifyPayment, enabled = !busy) { Text("Retry payment confirmation") }
        }
        state.busy?.takeIf { it != CLAIM_BUSY }?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniWheelSpinner(size = 20.dp)
                Text(it, style = MaterialTheme.typography.titleSmall)
            }
        }
        if (state.busy == "Waiting for payment…") {
            TextButton(onClick = vm::checkPaymentStatus) { Text("Check payment status") }
        }
        when {
            enrollment != null -> {
                val cycle = status.cycle
                val reward = cycle?.reward
                when {
                    cycle != null && cycle.won && reward != null -> {
                        WinnerCard(
                            cycle = cycle,
                            reward = reward,
                            claiming = state.busy == CLAIM_BUSY,
                            enabled = !busy && !state.loading,
                            justClaimed = state.justClaimed == cycle.month_key,
                            error = state.claimError,
                            onClaim = vm::claimReward,
                            onHowItWorks = null,
                        )
                        TextButton(enabled = !busy, onClick = { navController.openTab(Routes.ORDERS) }) {
                            Text("View orders and deliveries")
                        }
                    }
                    cycle != null -> RoundCard(cycle, enrollment, busy || state.loading,
                        onClaim = vm::claimReward, onOrders = { navController.openTab(Routes.ORDERS) },
                        onShop = { navController.openTab(Routes.SHOP) })
                    else -> DrawCard("You're in ${enrollment.scheme_name}") {
                        Text("Your round is being prepared. The draw dates and your monthly eligibility will appear here.")
                    }
                }
                cycle?.latest_draw?.takeIf { belongsToEnrollment(it, enrollment) && !state.unseenResult }?.let { result ->
                    DrawCard("Latest result · ${drawMonthName(result.month_key)}") { ResultText(result) }
                }
                HowItWorksCard(joiningFee = null, schedule = status.draw_schedule)
                enrollment.welcome_gift_order_id?.let { orderId ->
                    OutlinedButton(enabled = !busy, onClick = { navController.navigate(Routes.orderDetail(orderId)) },
                        modifier = Modifier.fillMaxWidth()) { Text("Track welcome gift") }
                }
                OutlinedButton(enabled = !busy, onClick = { navController.openTab(Routes.CONTAINERS) },
                    modifier = Modifier.fillMaxWidth()) { Text("Your containers") }
                TextButton(enabled = !busy && !state.loading, onClick = onLeave) {
                    Text(if (enrollment.status == "won") "Return container and leave round" else "Leave this round")
                }
            }
            status.open_schemes.isNotEmpty() -> EnrollmentForm(state, vm::enroll, vm::linkDealer,
                onAddAddress = { navController.navigate(Routes.ADDRESSES) })
            else -> {
                DrawCard("No rounds open right now") {
                    Text("The next round will appear here when enrollment opens. Past gifts and rewards are in your orders.")
                    TextButton(onClick = { navController.openTab(Routes.ORDERS) }) { Text("View orders") }
                }
                HowItWorksCard(joiningFee = null, schedule = status.draw_schedule)
            }
        }
        if (state.winners.isNotEmpty()) DrawCard("Recent winners") {
            state.winners.forEach { winner ->
                Text("${winner.display_name} · ${winner.scheme_name}" +
                    winner.reward_product_name?.let { "\nReceiving $it free" }.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun EnrollmentForm(state: LuckyDrawUiState, onEnroll: (DrawScheme, Boolean) -> Unit,
    onLinkDealer: (String) -> Unit, onAddAddress: () -> Unit) {
    val status = state.status ?: return
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var wantsGift by rememberSaveable { mutableStateOf(true) }
    var referral by rememberSaveable { mutableStateOf("") }
    var confirmLink by rememberSaveable { mutableStateOf(false) }
    val scheme = status.open_schemes.firstOrNull { it.id == selectedId } ?: status.open_schemes.first()
    val enabled = state.busy == null && !state.loading
    DrawCard("Choose your round") {
        status.open_schemes.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = option.id == scheme.id, enabled = enabled, onClick = { selectedId = option.id })
                Column {
                    Text(option.name, style = MaterialTheme.typography.titleSmall)
                    Text("${formatRupees(option.joining_fee)} once", style = MaterialTheme.typography.bodyMedium)
                    option.enrollment_closes_at?.let {
                        Text("Join before ${drawDate(it)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    HowItWorksCard(joiningFee = scheme.joining_fee, schedule = status.draw_schedule)
    if (state.consumer != null && state.consumer.linked_dealer_id == null) DrawCard("Link your trader") {
        Text("Enter your trader's referral code so they can deliver your welcome gift and any rewards.")
        OutlinedTextField(value = referral, onValueChange = { referral = it }, label = { Text("Trader referral code") },
            singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
        OutlinedButton(enabled = enabled && referral.isNotBlank(), onClick = { confirmLink = true }) { Text("Link trader") }
    }
    if (state.addresses?.isEmpty() == true) DrawCard("Add a delivery address") {
        Text("Save an address before paying so your trader knows where to deliver.")
        OutlinedButton(enabled = enabled, onClick = onAddAddress) { Text("Add address") }
    }
    DrawCard("Your welcome gift") {
        if (state.gift != null) {
            Row(Modifier.fillMaxWidth().toggleable(value = wantsGift, enabled = enabled,
                role = Role.Checkbox, onValueChange = { wantsGift = it }), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = wantsGift, onCheckedChange = null, enabled = enabled)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(state.gift.name, style = MaterialTheme.typography.titleSmall)
                    Text("Free trial bottle · no container deposit", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Optional — you can join with or without the gift.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("The welcome gift is unavailable. You can still join without it, or refresh to check again.")
        }
    }
    Button(modifier = Modifier.fillMaxWidth(), enabled = enabled && !state.verificationPending &&
        state.consumer?.linked_dealer_id != null && !state.addresses.isNullOrEmpty(),
        onClick = { onEnroll(scheme, wantsGift && state.gift != null) }) {
        Text("Join ${scheme.name} · ${formatRupees(scheme.joining_fee)}")
    }
    if (confirmLink) AlertDialog(onDismissRequest = { confirmLink = false },
        title = { Text("Link this trader?") },
        text = { Text("Code: ${referral.trim()}. This permanently links your account to that trader for deliveries. Check the code before continuing.") },
        confirmButton = { TextButton(onClick = { confirmLink = false; onLinkDealer(referral) }) { Text("Link trader") } },
        dismissButton = { TextButton(onClick = { confirmLink = false }) { Text("Cancel") } })
}

@Composable
internal fun RoundCard(cycle: DrawCycle, enrollment: DrawEnrollment, disabled: Boolean,
    onClaim: () -> Unit, onOrders: () -> Unit, onShop: () -> Unit) {
    DrawCard(if (cycle.won) "You've won ${cycle.scheme_name}" else "You're in ${cycle.scheme_name}") {
        Text("Month ${cycle.month_number} of ${cycle.total_months}", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = { cycle.month_number.toFloat().div(cycle.total_months.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth())
        when {
            cycle.round_over -> Text("Your round has finished. You can track any remaining deliveries in Orders.")
            cycle.won -> {
                val reward = cycle.reward
                Text("${reward?.quantity ?: 1} × ${reward?.product_name ?: enrollment.reward_product_name ?: "your reward oil"}",
                    style = MaterialTheme.typography.titleLarge)
                if (reward?.this_month_ordered == true) {
                    Text(if (reward.this_month_delivered) "Delivered for this month" else
                        "Ordered for this month · ${statusLabel(reward.this_month_delivery_status ?: "pending")}")
                    reward.this_month_order_number?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                } else if (cycle.canClaim) {
                    Text("Claim this month's free delivery. No purchase or new payment is required.")
                    Button(enabled = !disabled, onClick = onClaim, modifier = Modifier.fillMaxWidth()) { Text("Order this month's oil · ₹0") }
                }
                if (reward != null) Text("${reward.months_remaining} monthly deliveries remain after this month.",
                    style = MaterialTheme.typography.bodySmall)
            }
            else -> {
                Text(if (cycle.entered) "You're qualified for this month's draw" else "No qualifying entry for this month's draw yet",
                    style = MaterialTheme.typography.titleSmall)
                Text("Draw: ${drawDate(cycle.draw_at)}")
                Text("${cycle.tickets} ${if (cycle.tickets == 1) "entry" else "entries"} when qualified · ${cycle.qualified_months} qualifying months")
                cycle.entry_order_number?.takeIf { cycle.entered }?.let { Text("Qualifying order: $it", style = MaterialTheme.typography.bodySmall) }
                if (enrollment.scheme_status == "paused") Text("This round is paused. Your trader will let you know when it resumes.")
                checkoutDrawMessage(cycle)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Button(enabled = !disabled, onClick = onShop) { Text("Shop oils") }
            }
        }
        TextButton(enabled = !disabled, onClick = onOrders) { Text("View orders and deliveries") }
    }
}


@Composable
private fun ResultText(result: DrawResult) {
    Text(when {
        !result.has_winner -> "No winner this month"
        result.is_me -> "You won!"
        else -> "${result.winner_display_name ?: "A member of your round"} won"
    }, style = MaterialTheme.typography.headlineSmall)
    Text(when {
        !result.has_winner -> "No eligible entry was selected in this draw."
        result.is_me -> "Your reward oil is now free every month for the rest of this round. Claim each month's delivery from your round."
        else -> "${result.reward_product_name ?: "Their reward oil"} will be delivered free for the rest of the round."
    })
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
