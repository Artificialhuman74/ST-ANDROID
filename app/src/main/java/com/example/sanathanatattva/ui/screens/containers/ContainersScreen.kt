package com.example.sanathanatattva.ui.screens.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sanathanatattva.data.model.ContainerHolding
import com.example.sanathanatattva.data.model.SwappableProduct
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.DockContentClearance
import com.example.sanathanatattva.ui.components.EmptyState
import com.example.sanathanatattva.ui.components.ErrorState
import com.example.sanathanatattva.ui.components.SkeletonBlock
import com.example.sanathanatattva.ui.components.TintedIcon
import com.example.sanathanatattva.ui.components.cardBorder
import com.example.sanathanatattva.ui.components.TopSnackbarHost
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.navigation.openTab
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.util.formatRupees

private fun holdingStatusLabel(status: String): String = when (status) {
    "held" -> "In your care"
    "pending_delivery" -> "Arriving with your order"
    "refund_requested" -> "Refund requested"
    "swapped" -> "Swapped"
    "forfeited" -> "Forfeited"
    "returned" -> "Returned"
    else -> status.replaceFirstChar(Char::uppercase)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersScreen(navController: NavController, session: SessionViewModel) {
    val container = appContainer()
    val vm: ContainersViewModel = viewModel { ContainersViewModel(container) }
    val state by vm.state.collectAsState()
    val isLoggedIn by session.isLoggedIn.collectAsState()
    val steelSheetSeen by vm.steelSheetSeen.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var tab by remember { mutableIntStateOf(0) }
    var refundTarget by remember { mutableStateOf<ContainerHolding?>(null) }
    var swapTarget by remember { mutableStateOf<ContainerHolding?>(null) }
    var disputeTarget by remember { mutableStateOf<ContainerHolding?>(null) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == true) vm.open()
    }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your containers") }) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
        when {
            isLoggedIn == false -> EmptyState(
                icon = Icons.Outlined.Inventory2,
                title = "Log in to see your containers",
                body = "Oil comes in returnable containers. Your deposits and refills are tracked here.",
                actionLabel = "Log in",
                onAction = { navController.navigate(Routes.LOGIN) },
                modifier = Modifier.padding(padding),
            )

            state.loading && state.held.isEmpty() && state.history.isEmpty() -> Column(
                Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(3) { SkeletonBlock(Modifier.fillMaxWidth(), height = 110.dp, corner = 16.dp) }
            }

            state.error != null -> ErrorState(
                message = state.error!!,
                onRetry = { vm.load() },
                modifier = Modifier.padding(padding),
            )

            else -> Column(Modifier.padding(padding)) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Held") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("History") })
                }

                val list = if (tab == 0) state.held else state.history
                if (list.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Inventory2,
                        title = if (tab == 0) "No containers with you" else "No container history",
                        body = "When you buy oil, the container is yours to keep refilling. " +
                            "Return it any time and the deposit comes back to you.",
                        actionLabel = if (tab == 0) "Browse the shop" else null,
                        onAction = if (tab == 0) {
                            { navController.openTab(Routes.SHOP) }
                        } else null,
                    )
                } else {
                    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = { vm.load(pulled = true) }) {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 16.dp + DockContentClearance,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(list, key = { it.id }) { holding ->
                                HoldingCard(
                                    holding = holding,
                                    actionInProgress = state.actionInProgress,
                                    onRequestRefund = { refundTarget = holding },
                                    onCancelRefund = { vm.cancelRefund(holding.id) },
                                    onSwap = { swapTarget = holding },
                                    onDispute = { disputeTarget = holding },
                                    onRewardRound = { navController.navigate(Routes.LUCKY_DRAW) },
                                    rewardRoundActive = state.rewardRoundActive,
                                )
                            }
                        }
                    }
                }
            }
        }

            // In-app notifications drop in from the top, not the bottom.
            TopSnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = padding.calculateTopPadding()),
            )
        }
    }

    // Welcome story: shown exactly once, first time any user opens this tab.
    // steelSheetSeen == null → DataStore still loading; don't show yet.
    // steelSheetSeen == false → first visit; show the story.
    if (steelSheetSeen == false) {
        SteelStorySheet(
            onDismiss = { vm.markSteelSheetSeen() },
        )
    }

    refundTarget?.let { holding ->
        RefundDialog(
            holding = holding,
            onConfirm = { destination ->
                vm.requestRefund(holding.id, destination)
                refundTarget = null
            },
            onDismiss = { refundTarget = null },
        )
    }

    swapTarget?.let { holding ->
        SwapDialog(
            holding = holding,
            options = state.swappable.filter {
                it.container_type == holding.container_type && it.id != holding.current_product_id
            },
            onConfirm = { productId ->
                vm.swap(holding.id, productId)
                swapTarget = null
            },
            onDismiss = { swapTarget = null },
        )
    }

    disputeTarget?.let { holding ->
        DisputeDialog(
            onConfirm = { notes ->
                vm.dispute(holding.id, notes)
                disputeTarget = null
            },
            onDismiss = { disputeTarget = null },
        )
    }
}

@Composable
private fun HoldingCard(
    holding: ContainerHolding,
    actionInProgress: Boolean,
    onRequestRefund: () -> Unit,
    onCancelRefund: () -> Unit,
    onSwap: () -> Unit,
    onDispute: () -> Unit,
    onRewardRound: () -> Unit,
    rewardRoundActive: Boolean?,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = cardBorder(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TintedIcon(
                    icon = Icons.Outlined.Inventory2,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        holding.current_product_name ?: "Container",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${holding.container_type} container",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    // Deposit is a money moment → ledger gold.
                    Text(
                        formatRupees(holding.deposit_amount),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        if (holding.source == "lucky_draw_reward") "reward container" else "deposit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val (statusBg, statusFg) = when (holding.status) {
                "held" -> MaterialTheme.colorScheme.primaryContainer to
                    MaterialTheme.colorScheme.onPrimaryContainer
                "forfeited" -> MaterialTheme.colorScheme.errorContainer to
                    MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.secondaryContainer to
                    MaterialTheme.colorScheme.onSecondaryContainer
            }
            Text(
                holdingStatusLabel(holding.status),
                style = MaterialTheme.typography.labelMedium,
                color = statusFg,
                modifier = Modifier
                    .background(statusBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )

            if (holding.source == "lucky_draw_reward") {
                Spacer(Modifier.height(10.dp))
                Text(if (rewardRoundActive == false) "Won with Bhagya Chakra. No deposit was charged for this container."
                    else "Won with Bhagya Chakra. Manage your free refills and return this container from your round.",
                    style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onRewardRound, enabled = !actionInProgress) {
                    Text("View rewards and returns")
                }
            }

            when (holding.status) {
                "held" -> {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (holding.source != "lucky_draw_reward" || rewardRoundActive == false) {
                            OutlinedButton(onClick = onRequestRefund, enabled = !actionInProgress) {
                                Text(if (holding.source == "lucky_draw_reward") "Return container" else "Return for refund")
                            }
                        }
                        if (holding.source != "lucky_draw_reward") {
                            OutlinedButton(onClick = onSwap, enabled = !actionInProgress) { Text("Swap oil") }
                        }
                    }
                }

                "refund_requested" -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (holding.source == "lucky_draw_reward") "Your trader will collect this reward container."
                        else "We'll arrange a pickup. The deposit is refunded after the container is collected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (holding.source != "lucky_draw_reward") OutlinedButton(onClick = onCancelRefund, enabled = !actionInProgress) {
                        Text("Keep the container")
                    }
                }

                "forfeited" -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "This container was marked forfeited. If that's wrong, open a dispute within 48 hours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onDispute, enabled = !actionInProgress) {
                        Text("Open a dispute")
                    }
                }
            }
        }
    }
}

@Composable
private fun RefundDialog(
    holding: ContainerHolding,
    onConfirm: (destination: String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Return ${holding.container_type} container") },
        text = {
            Text(
                if (holding.source == "lucky_draw_reward") "Your trader will collect this reward container. No deposit was charged, so no refund is due."
                else "Your ${formatRupees(holding.deposit_amount)} deposit comes back to you. " +
                    "Store credit is instant and can be used on your next order. " +
                    "A bank refund is arranged manually and can take a few days."
            )
        },
        confirmButton = {
            Column {
                Button(onClick = { onConfirm("store_credit") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (holding.source == "lucky_draw_reward") "Request return" else "Refund as store credit")
                }
                Spacer(Modifier.height(8.dp))
                if (holding.source != "lucky_draw_reward") OutlinedButton(onClick = { onConfirm("manual_bank") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Refund to my bank")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SwapDialog(
    holding: ContainerHolding,
    options: List<SwappableProduct>,
    onConfirm: (productId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Swap to a different oil") },
        text = {
            if (options.isEmpty()) {
                Text("No other oils come in a ${holding.container_type} container right now.")
            } else {
                Column {
                    Text(
                        "Same ${holding.container_type} container, different oil on your next refill.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    options.forEach { option ->
                        OutlinedButton(
                            onClick = { onConfirm(option.id) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Text("${option.name} · ${formatRupees(option.price)}")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DisputeDialog(
    onConfirm: (notes: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open a dispute") },
        text = {
            Column {
                Text("Tell us what happened with this container. Our team will get back to you.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 1000) notes = it },
                    label = { Text("What happened?") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(notes) }) { Text("Submit dispute") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
