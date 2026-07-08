package com.example.sanathanatattva.ui.screens.checkout

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.util.formatRupees
import com.razorpay.Checkout
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(navController: NavController, session: SessionViewModel) {
    val container = appContainer()
    val vm: CheckoutViewModel = viewModel { CheckoutViewModel(container) }
    val state by vm.state.collectAsState()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (stage is CheckoutStage.Success) "Order placed" else "Checkout") },
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
            is CheckoutStage.Success -> SuccessContent(
                stage = stage,
                onTrackOrder = {
                    navController.navigate(Routes.ORDERS) {
                        popUpTo(Routes.SHOP) { inclusive = false }
                    }
                },
                onBackToShop = {
                    navController.popBackStack(Routes.SHOP, inclusive = false)
                },
                modifier = Modifier.padding(padding),
            )

            CheckoutStage.PlacingOrder, CheckoutStage.Verifying -> Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (stage == CheckoutStage.Verifying) "Confirming your payment…"
                        else "Placing your order…",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            else -> FormContent(vm, state, Modifier.padding(padding))
        }
    }
}

@Composable
private fun FormContent(vm: CheckoutViewModel, state: CheckoutUiState, modifier: Modifier) {
    val est = vm.estimate()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                        Row {
                            Text(address.label, style = MaterialTheme.typography.titleSmall)
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

        // ---- Referral code ----
        SectionCard(title = "Referral code") {
            OutlinedTextField(
                value = state.referralCode,
                onValueChange = vm::setReferralCode,
                label = { Text("Dealer code (optional)") },
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
                        else -> Text("Orders through a dealer are discounted.")
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
                .height(52.dp),
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
            onValueChange = { onChange(inline.copy(phone = it)) },
            label = { Text("Phone") },
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
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SuccessContent(
    stage: CheckoutStage.Success,
    onTrackOrder: () -> Unit,
    onBackToShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Order ${stage.orderNumber} placed", style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        if (stage.dealerName != null) {
            Text(
                "${stage.dealerName} will deliver your order." +
                    (stage.dealerPhone?.let { " You can reach them on $it." } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                "We've received your order and will assign a delivery partner shortly.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onTrackOrder,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) { Text("Track order") }
        TextButton(onClick = onBackToShop) { Text("Back to shop") }
    }
}
