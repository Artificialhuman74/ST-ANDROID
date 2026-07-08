package com.example.sanathanatattva.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sanathanatattva.data.model.Order
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.EmptyState
import com.example.sanathanatattva.ui.components.ErrorState
import com.example.sanathanatattva.ui.components.SkeletonBlock
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.util.formatRupees

/** Human labels for order statuses. */
fun statusLabel(status: String): String = when (status) {
    "pending" -> "Pending"
    "confirmed" -> "Confirmed"
    "processing" -> "Being prepared"
    "packed" -> "Packed"
    "dispatched" -> "Dispatched"
    "out_for_delivery" -> "Out for delivery"
    "delivered" -> "Delivered"
    "cancelled" -> "Cancelled"
    else -> status.replaceFirstChar(Char::uppercase)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(navController: NavController, session: SessionViewModel) {
    val container = appContainer()
    val vm: OrdersViewModel = viewModel { OrdersViewModel(container) }
    val state by vm.state.collectAsState()
    val isLoggedIn by session.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == true) vm.load()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your orders") }) },
    ) { padding ->
        when {
            isLoggedIn == false -> EmptyState(
                icon = Icons.Outlined.ReceiptLong,
                title = "Log in to see your orders",
                body = "Your orders and delivery updates will appear here.",
                actionLabel = "Log in",
                onAction = { navController.navigate(Routes.LOGIN) },
                modifier = Modifier.padding(padding),
            )

            state.loading && state.orders.isEmpty() -> Column(
                Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(3) { SkeletonBlock(Modifier.fillMaxWidth(), height = 100.dp, corner = 16.dp) }
            }

            state.error != null && state.orders.isEmpty() -> ErrorState(
                message = state.error!!,
                onRetry = vm::load,
                modifier = Modifier.padding(padding),
            )

            state.orders.isEmpty() -> EmptyState(
                icon = Icons.Outlined.ReceiptLong,
                title = "No orders yet",
                body = "When you place your first order, its delivery progress will show up here.",
                actionLabel = "Browse the shop",
                onAction = { navController.navigate(Routes.SHOP) },
                modifier = Modifier.padding(padding),
            )

            else -> PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = vm::load,
                modifier = Modifier.padding(padding),
            ) {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.orders, key = { it.id }) { order ->
                        OrderCard(order) {
                            navController.navigate(Routes.orderDetail(order.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text(order.order_number, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                StatusChip(order.status)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                order.items.joinToString { "${it.quantity} × ${it.product_name}" }
                    .ifBlank { "${order.item_count} items" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Spacer(Modifier.height(6.dp))
            Row {
                Text(
                    order.created_at.take(10),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(formatRupees(order.total_amount), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (bg, fg) = when (status) {
        "delivered" -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        "cancelled" -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
    }
    Text(
        statusLabel(status),
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
