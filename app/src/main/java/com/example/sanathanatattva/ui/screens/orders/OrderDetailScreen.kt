package com.example.sanathanatattva.ui.screens.orders

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sanathanatattva.imageUrl
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.ErrorState
import com.example.sanathanatattva.ui.components.cardBorder
import com.example.sanathanatattva.ui.components.SkeletonBlock
import com.example.sanathanatattva.util.formatRupees
import com.example.sanathanatattva.ui.screens.luckydraw.LuckyDrawOrderBadge

private val PROGRESS_STEPS = listOf(
    "confirmed", "processing", "packed", "dispatched", "out_for_delivery", "delivered",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(navController: NavController, orderId: Long) {
    val container = appContainer()
    val vm: OrderDetailViewModel = viewModel { OrderDetailViewModel(container, orderId) }
    val state by vm.state.collectAsState()
    // Tracking checks the server only while this screen is in front.
    LifecycleResumeEffect(vm) {
        vm.setOnScreen(true)
        onPauseOrDispose { vm.setOnScreen(false) }
    }
    val callEvent by vm.callEvents.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(callEvent) {
        when (val e = callEvent) {
            is CallEvent.Dial -> context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${e.phone}".toUri()))
            is CallEvent.Failed -> snackbar.showSnackbar(e.message)
            null -> return@LaunchedEffect
        }
        vm.consumeCall()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.order?.order_number ?: "Order",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val order = state.order
        when {
            state.loading -> Column(
                Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SkeletonBlock(Modifier.fillMaxWidth(), height = 200.dp, corner = 16.dp)
                SkeletonBlock(Modifier.fillMaxWidth(), height = 120.dp, corner = 16.dp)
            }

            state.error != null -> ErrorState(
                message = state.error!!,
                onRetry = vm::load,
                modifier = Modifier.padding(padding),
            )

            order != null -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ---- Live tracking: where it is, who has it, what happened ----
                // Needs the server's history (Oct 2026); an older server keeps today's cards.
                val tracking = state.tracking?.takeIf { it.events.isNotEmpty() }
                if (tracking != null) {
                    LiveTrackingSection(
                        tracking = tracking,
                        trackedAt = state.trackedAt,
                        route = state.route,
                        calling = state.calling,
                        onCall = vm::call,
                    )
                }
                LuckyDrawOrderBadge(order.order_number)

                // Older servers without tracking: the plain status steps and "Delivered by".
                if (tracking == null) Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                    border = cardBorder(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Delivery progress", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            StatusChip(order.status)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (order.status == "cancelled") {
                            Text(
                                "This order was cancelled." +
                                    if (order.payment_status == "paid")
                                        " Any payment will be refunded to the original method."
                                    else "",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            val reachedIndex = PROGRESS_STEPS.indexOf(order.status)
                            PROGRESS_STEPS.forEachIndexed { i, step ->
                                TimelineRow(
                                    label = statusLabel(step),
                                    done = reachedIndex >= i,
                                    current = reachedIndex == i,
                                    last = i == PROGRESS_STEPS.lastIndex,
                                )
                            }
                        }
                    }
                }

                // ---- Delivery person ----
                val deliveryName = order.delivery_dealer_name ?: order.dealer_name
                val deliveryPhone = order.delivery_dealer_phone ?: order.dealer_phone
                if (tracking == null && deliveryName != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                        border = cardBorder(),
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Delivered by", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(deliveryName, style = MaterialTheme.typography.titleSmall)
                                deliveryPhone?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (deliveryPhone != null) {
                                FilledTonalButton(onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, "tel:$deliveryPhone".toUri())
                                    )
                                }) {
                                    Icon(Icons.Filled.Call, contentDescription = null,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Call")
                                }
                            }
                        }
                    }
                }

                // ---- Items ----
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                    border = cardBorder(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Items", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        state.items.forEach { item ->
                            Row(
                                Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl(item.image_url))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.product_name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.product_name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        buildString {
                                            append("${item.quantity} × ${formatRupees(item.price)}")
                                            if (item.is_refill == 1) append(" · refill")
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(formatRupees(item.total), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        BreakdownRow("Subtotal (incl. GST)", order.subtotal)
                        // Discount, deposits and store credit are money moments → ledger gold.
                        if (order.discount_amount > 0)
                            BreakdownRow(
                                "Discount (${order.discount_percent.toInt()}%)",
                                -order.discount_amount,
                                highlight = true,
                            )
                        if (order.container_costs_total > 0)
                            BreakdownRow("Container deposits", order.container_costs_total, highlight = true)
                        if (order.store_credit_applied > 0)
                            BreakdownRow("Store credit", -order.store_credit_applied, highlight = true)
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row {
                            Text("Paid", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            Text(formatRupees(order.total_amount), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // ---- Address ----
                order.delivery_address?.let { address ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                        border = cardBorder(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Delivery address", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$address${order.pincode?.let { ", $it" } ?: ""}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TimelineRow(label: String, done: Boolean, current: Boolean, last: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            if (!last) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(22.dp)
                        .background(
                            if (done) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = if (current) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.bodyMedium,
            color = if (done) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Double, highlight: Boolean = false) {
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
