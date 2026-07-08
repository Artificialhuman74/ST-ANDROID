package com.example.sanathanatattva.ui.screens.containers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Brightness7
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sanathanatattva.data.model.ContainerHolding
import com.example.sanathanatattva.data.model.SwappableProduct
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.EmptyState
import com.example.sanathanatattva.ui.components.ErrorState
import com.example.sanathanatattva.ui.components.SkeletonBlock
import com.example.sanathanatattva.ui.navigation.Routes
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
        if (isLoggedIn == true) vm.load()
    }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your containers") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
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
                onRetry = vm::load,
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
                            { navController.navigate(Routes.SHOP) }
                        } else null,
                    )
                } else {
                    PullToRefreshBox(isRefreshing = state.loading, onRefresh = vm::load) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(list, key = { it.id }) { holding ->
                                HoldingCard(
                                    holding = holding,
                                    onRequestRefund = { refundTarget = holding },
                                    onCancelRefund = { vm.cancelRefund(holding.id) },
                                    onSwap = { swapTarget = holding },
                                    onDispute = { disputeTarget = holding },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Welcome sheet: shown exactly once, first time any user opens this tab.
    // steelSheetSeen == null → DataStore still loading; don't show yet.
    // steelSheetSeen == false → first visit; show the sheet.
    if (steelSheetSeen == false) {
        SteelContainerSheet(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SteelContainerSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Text(
                    "Cold-pressed oils need the right home.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Most of what makes these oils worth buying — the fatty acids, the antioxidants, the flavour — " +
                        "breaks down in the wrong container. Here's why we use steel.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))
            }

            item {
                SteelPoint(
                    icon = Icons.Outlined.Brightness7,
                    title = "Blocks all light",
                    body = "Oils contain trace photosensitisers — chlorophyll, riboflavin — that absorb sunlight " +
                        "and transfer that energy to oxygen. The result is singlet oxygen: 1,500× more reactive " +
                        "than ordinary oxygen, attacking fatty acids in seconds. Steel is opaque. Plastic isn't.",
                )
                Spacer(Modifier.height(20.dp))
            }

            item {
                SteelPoint(
                    icon = Icons.Outlined.Air,
                    title = "Sealed against oxygen",
                    body = "Plastic is a membrane, not a wall. Oxygen diffuses through its molecular structure " +
                        "over weeks, turning fats rancid and destroying the antioxidants that make cold-pressed " +
                        "oil worth buying. Steel is hermetic — what you seal in, stays in.",
                )
                Spacer(Modifier.height(20.dp))
            }

            item {
                SteelPoint(
                    icon = Icons.Outlined.WaterDrop,
                    title = "Nothing leaches in",
                    body = "Cold-pressed oils are fat-soluble. Fats draw compounds out of plastic — plasticisers, " +
                        "BPA-analogues — that don't belong in food. Steel is inert. What goes into the container " +
                        "is exactly what comes out.",
                )
                Spacer(Modifier.height(20.dp))
            }

            item {
                SteelPoint(
                    icon = Icons.Outlined.Lock,
                    title = "Odour stays out",
                    body = "Plastic absorbs odour molecules and re-releases them later — a phenomenon called " +
                        "flavour scalping. Your sesame oil shouldn't carry a trace of the coconut oil from the " +
                        "same rack. In steel, it doesn't.",
                )
                Spacer(Modifier.height(28.dp))
            }

            item {
                SteelVsPlasticStrip()
                Spacer(Modifier.height(28.dp))
            }

            item {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Got it")
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SteelPoint(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SteelVsPlasticStrip() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Steel vs. plastic",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            ComparisonRow("Blocks light", steel = true)
            ComparisonRow("Oxygen barrier", steel = true)
            ComparisonRow("Chemical inertness", steel = true)
            ComparisonRow("Odour neutrality", steel = true)
        }
    }
}

@Composable
private fun ComparisonRow(label: String, steel: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            "Steel ✓",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            "Plastic ✗",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HoldingCard(
    holding: ContainerHolding,
    onRequestRefund: () -> Unit,
    onCancelRefund: () -> Unit,
    onSwap: () -> Unit,
    onDispute: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(
                        holding.current_product_name ?: "Container",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "${holding.container_type} container",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatRupees(holding.deposit_amount),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        "deposit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                holdingStatusLabel(holding.status),
                style = MaterialTheme.typography.labelLarge,
                color = when (holding.status) {
                    "held" -> MaterialTheme.colorScheme.primary
                    "forfeited" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            when (holding.status) {
                "held" -> {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onRequestRefund) { Text("Return for refund") }
                        OutlinedButton(onClick = onSwap) { Text("Swap oil") }
                    }
                }

                "refund_requested" -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "We'll arrange a pickup. The deposit is refunded after the container is collected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onCancelRefund) { Text("Keep the container") }
                }

                "forfeited" -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "This container was marked forfeited. If that's wrong, open a dispute within 48 hours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onDispute) { Text("Open a dispute") }
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
                "Your ${formatRupees(holding.deposit_amount)} deposit comes back to you. " +
                    "Store credit is instant and can be used on your next order. " +
                    "A bank refund is arranged manually and can take a few days."
            )
        },
        confirmButton = {
            Column {
                Button(onClick = { onConfirm("store_credit") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Refund as store credit")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onConfirm("manual_bank") }, modifier = Modifier.fillMaxWidth()) {
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
