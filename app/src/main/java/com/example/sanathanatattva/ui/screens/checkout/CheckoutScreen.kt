package com.example.sanathanatattva.ui.screens.checkout

import com.example.sanathanatattva.util.mobileDigits
import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.brand.OrderSealMedallion
import com.example.sanathanatattva.ui.brand.SealLanding
import com.example.sanathanatattva.ui.brand.SealMoment
import com.example.sanathanatattva.ui.brand.SealOrigin
import com.example.sanathanatattva.ui.brand.orderPlacedMoment
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.navigation.openTab
import com.example.sanathanatattva.ui.components.EaseOutQuint
import com.example.sanathanatattva.ui.components.TintedIcon
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.components.cardBorder
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.ui.screens.luckydraw.LuckyDrawCheckoutNote
import com.example.sanathanatattva.util.formatRupees
import com.razorpay.Checkout
import kotlinx.coroutines.delay
import org.json.JSONObject

/** Progress of the "order placed" celebration, as far as this screen is concerned. */
private enum class OrderSeal { Idle, Playing, Revealed }

private val SuccessMedallionDiameter = 72.dp

/** When the success page starts rising in under a celebration: just after the green begins to settle. */
private const val SUCCESS_ENTRANCE_DELAY_MS = 1180

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    session: SessionViewModel,
    celebrate: (SealMoment) -> Unit,
) {
    val container = appContainer()
    val vm: CheckoutViewModel = viewModel { CheckoutViewModel(container) }
    val state by vm.state.collectAsState()
    val consumer by session.consumer.collectAsState()
    val context = LocalContext.current

    // Open the Razorpay sheet when the ViewModel asks for it.
    LaunchedEffect(Unit) {
        vm.openRazorpay.collect { payload ->
            val activity = context as? Activity ?: return@collect
            val checkout = Checkout()
            checkout.setKeyID(payload.keyId)
            val options = JSONObject().apply {
                put("name", "Sanathana Tattva")
                put("description", "Order ${payload.orderNumber}")
                put("order_id", payload.razorpayOrderId)
                put("currency", payload.currency)
                put("amount", payload.amountPaise)
                put("theme", JSONObject().put("color", "#1A6B2E"))
                put("prefill", JSONObject().apply {
                    payload.name?.let { put("name", it) }
                    payload.email?.let { put("email", it) }
                    payload.phone?.let { put("contact", it) }
                })
            }
            runCatching { checkout.open(activity, options) }
                .onFailure { vm.onPaymentDismissed() }
        }
    }

    val stage = state.stage

    // Payment confirmed → "one drop seals it": the confirming spinner becomes a drop,
    // the green covers the screen, the success page is swapped in underneath, and the
    // green settles as that page's medallion.
    val reducedMotion = rememberReducedMotion()
    var seal by remember { mutableStateOf(OrderSeal.Idle) }
    var sawVerifying by remember { mutableStateOf(false) }
    var spinnerBounds by remember { mutableStateOf<Rect?>(null) }
    var medallionCenter by remember { mutableStateOf<Offset?>(null) }
    val spinnerTint = MaterialTheme.colorScheme.primary
    val celebrating = stage is CheckoutStage.Success &&
        (seal == OrderSeal.Playing || (seal == OrderSeal.Idle && sawVerifying && spinnerBounds != null))

    LaunchedEffect(stage) {
        if (stage == CheckoutStage.Verifying) sawVerifying = true
        val origin = spinnerBounds
        if (stage is CheckoutStage.Success && seal == OrderSeal.Idle && sawVerifying && origin != null) {
            seal = OrderSeal.Playing
            celebrate(
                orderPlacedMoment(
                    origin = SealOrigin.Spinner(origin, spinnerTint),
                    orderNumber = stage.orderNumber,
                    medallion = {
                        medallionCenter?.let { SealLanding.Medallion(it, SuccessMedallionDiameter / 2) }
                    },
                    onCovered = { seal = OrderSeal.Revealed },
                ),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (stage is CheckoutStage.Success && !celebrating) "Order placed" else "Checkout")
                },
                navigationIcon = {
                    if (stage !is CheckoutStage.Success) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (stage) {
            is CheckoutStage.Success -> if (celebrating) {
                // The spinner has become the celebration's drop; hold this view until
                // the green covers it.
                ProgressContent(
                    message = "Confirming your payment…",
                    spinnerVisible = false,
                    onSpinnerPlaced = {},
                    modifier = Modifier.padding(padding),
                )
            } else {
                SuccessContent(
                    stage = stage,
                    onTrackOrder = {
                        navController.openTab(Routes.ORDERS)
                    },
                    onBackToShop = {
                        navController.popBackStack(Routes.SHOP, inclusive = false)
                    },
                    onMedallionPlaced = { medallionCenter = it },
                    entranceDelayMillis = if (seal == OrderSeal.Revealed && !reducedMotion) {
                        SUCCESS_ENTRANCE_DELAY_MS
                    } else {
                        0
                    },
                    modifier = Modifier.padding(padding),
                )
            }

            CheckoutStage.PlacingOrder, CheckoutStage.Verifying -> ProgressContent(
                message = if (stage == CheckoutStage.Verifying) "Confirming your payment…"
                else "Placing your order…",
                spinnerVisible = true,
                onSpinnerPlaced = { spinnerBounds = it },
                modifier = Modifier.padding(padding),
            )

            else -> FormContent(vm, state, consumer?.dealer, Modifier.padding(padding), session)
        }
    }
}

@Composable
private fun FormContent(
    vm: CheckoutViewModel,
    state: CheckoutUiState,
    partner: com.example.sanathanatattva.data.model.LinkedDealer?,
    modifier: Modifier,
    session: SessionViewModel,
) {
    val est = vm.estimate()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.loading) {
            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        // ---- Delivery address ----
        LuckyDrawCheckoutNote(session)
        SectionCard(title = "Delivery address") {
            state.addresses.forEach { address ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = !state.useInlineAddress && state.selectedAddressId == address.id,
                            onClick = { vm.selectAddress(address.id) },
                        )
                        .padding(vertical = 6.dp),
                ) {
                    RadioButton(
                        selected = !state.useInlineAddress && state.selectedAddressId == address.id,
                        onClick = { vm.selectAddress(address.id) },
                    )
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                address.label,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (address.is_default == 1) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Default",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Text(
                            "${address.name} · ${address.phone}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "${address.address}, ${address.pincode}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.addresses.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.useInlineAddress,
                            onClick = { vm.useInlineAddress(true) },
                        )
                        .padding(vertical = 6.dp),
                ) {
                    RadioButton(
                        selected = state.useInlineAddress,
                        onClick = { vm.useInlineAddress(true) },
                    )
                    Text("Deliver somewhere else", style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (state.useInlineAddress) {
                InlineAddressForm(state.inline, onChange = vm::updateInline)
            }
        }

        // ---- Your partner (the local seller who fulfils this order) ----
        partner?.name?.let { partnerName ->
            SectionCard(title = "Your partner") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TintedIcon(
                        icon = Icons.Outlined.Storefront,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(partnerName, style = MaterialTheme.typography.titleMedium)
                        partner.phone?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // ---- Referral code ----
        SectionCard(title = "Referral code") {
            OutlinedTextField(
                value = state.referralCode,
                onValueChange = vm::setReferralCode,
                label = { Text("Partner code (optional)") },
                singleLine = true,
                supportingText = {
                    when {
                        state.referralChecking -> Text("Checking code…")
                        state.referralDealerName != null -> Text(
                            "Ordering through ${state.referralDealerName}. " +
                                "${state.referralDiscountPercent.toInt()}% discount applied.",
                            color = MaterialTheme.colorScheme.primary,
                        )
                        state.referralCode.isNotBlank() -> Text("Code not recognised.")
                        else -> Text("Orders through a partner are discounted.")
                    }
                },
                trailingIcon = {
                    if (state.referralDealerName != null) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Valid code",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ---- Order summary ----
        SectionCard(title = "Order summary") {
            vm.cartLines().forEach { line ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(line.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            buildString {
                                append("${line.quantity} × ${formatRupees(line.price)}")
                                if (line.isRefill) append(" · refill")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        formatRupees(line.price * line.quantity),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SummaryRow("Subtotal (incl. GST)", est.subtotal)
            if (est.discount > 0) SummaryRow("Referral discount", -est.discount, highlight = true)
            if (est.deposits > 0) SummaryRow("Container deposits (refundable)", est.deposits)
            if (est.creditApplied > 0) SummaryRow("Store credit", -est.creditApplied, highlight = true)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row {
                Text("To pay", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(formatRupees(est.total), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Final amount is confirmed at payment.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---- Wallet credit ----
        if (state.walletBalance > 0) {
            SectionCard(title = "Store credit") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Use ${formatRupees(state.walletBalance)} store credit",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "From container refunds and cashback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.useWalletCredit,
                        onCheckedChange = vm::setUseWalletCredit,
                    )
                }
            }
        }

        // ---- Notes ----
        OutlinedTextField(
            value = state.notes,
            onValueChange = vm::setNotes,
            label = { Text("Delivery notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = vm::placeOrderAndPay,
            enabled = state.stage == CheckoutStage.Form,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text("Pay ${formatRupees(est.total)}")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InlineAddressForm(inline: InlineAddress, onChange: (InlineAddress) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = inline.name,
            onValueChange = { onChange(inline.copy(name = it)) },
            label = { Text("Receiver's name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = inline.phone,
            onValueChange = { onChange(inline.copy(phone = mobileDigits(it))) },
            label = { Text("Phone") },
            prefix = { Text("+91 ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = inline.address,
            onValueChange = { onChange(inline.copy(address = it)) },
            label = { Text("Full address") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = inline.pincode,
            onValueChange = { onChange(inline.copy(pincode = it.filter(Char::isDigit).take(6))) },
            label = { Text("PIN code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = inline.save,
                onCheckedChange = { onChange(inline.copy(save = it)) },
            )
            Text("Save this address for next time", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: Double, highlight: Boolean = false) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            (if (amount < 0) "− " else "") + formatRupees(kotlin.math.abs(amount)),
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(16.dp),
        border = cardBorder(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** "Placing your order…" / "Confirming your payment…" — a centred spinner and a line. */
@Composable
private fun ProgressContent(
    message: String,
    spinnerVisible: Boolean,
    onSpinnerPlaced: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messageAlpha by animateFloatAsState(
        targetValue = if (spinnerVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "progress-message",
    )
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(40.dp)
                    .onGloballyPositioned { onSpinnerPlaced(it.boundsInRoot()) },
            ) {
                if (spinnerVisible) CircularProgressIndicator(Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.alpha(messageAlpha),
            )
        }
    }
}

@Composable
private fun SuccessContent(
    stage: CheckoutStage.Success,
    onTrackOrder: () -> Unit,
    onBackToShop: () -> Unit,
    onMedallionPlaced: (Offset) -> Unit,
    entranceDelayMillis: Int,
    modifier: Modifier = Modifier,
) {
    // After a celebration the medallion is already in place (the green settled into
    // it); the words and actions rise in around it, one after another.
    val entrance = remember { Animatable(if (entranceDelayMillis > 0) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (entranceDelayMillis > 0) {
            delay(entranceDelayMillis.toLong())
            entrance.animateTo(1f, tween(560, easing = LinearEasing))
        }
    }
    fun Modifier.riseIn(step: Int) = graphicsLayer {
        val p = EaseOutQuint.transform(((entrance.value * 560f - 80f * step) / 400f).coerceIn(0f, 1f))
        alpha = p
        translationY = (1f - p) * 10.dp.toPx()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        OrderSealMedallion(
            diameter = SuccessMedallionDiameter,
            modifier = Modifier.onGloballyPositioned { onMedallionPlaced(it.boundsInRoot().center) },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Order ${stage.orderNumber} placed",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.riseIn(0),
        )
        Spacer(Modifier.height(8.dp))
        if (stage.dealerName != null) {
            Text(
                "${stage.dealerName} will deliver your order." +
                    (stage.dealerPhone?.let { " You can reach them on $it." } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.riseIn(1),
            )
        } else {
            Text(
                "We've received your order and will assign a delivery partner shortly.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.riseIn(1),
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onTrackOrder,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .riseIn(2),
        ) { Text("Track order") }
        TextButton(onClick = onBackToShop, modifier = Modifier.riseIn(2)) { Text("Back to shop") }
    }
}
