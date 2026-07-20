package com.example.sanathanatattva.ui.screens.cart

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sanathanatattva.data.cart.CartLine
import com.example.sanathanatattva.data.cart.CartTotals
import com.example.sanathanatattva.data.cart.totals
import com.example.sanathanatattva.imageUrl
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.EmptyState
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.util.formatRupees

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavController, session: SessionViewModel) {
    val container = appContainer()
    val vm: CartViewModel = viewModel { CartViewModel(container) }
    val lines by vm.lines.collectAsState()
    val caps by vm.refillCaps.collectAsState()
    val isLoggedIn by session.isLoggedIn.collectAsState()
    val reducedMotion = rememberReducedMotion()
    var priceDetailsExpanded by remember { mutableStateOf(false) }

    val totals = lines.totals()
    // What refilling instead of buying a fresh container is saving the customer right now.
    val depositSaved = lines.filter { it.isRefill }.sumOf { it.containerCost * it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your cart") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (lines.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .navigationBarsPadding(),
                    ) {
                        Row {
                            Text("To pay", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatRupees(totals.payable),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        if (totals.depositTotal > 0) {
                            Text(
                                "includes ${formatRupees(totals.depositTotal)} in refundable container deposits",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val destination = if (isLoggedIn == true) Routes.CHECKOUT else Routes.LOGIN
                                // launchSingleTop guards a rapid double-tap from pushing the
                                // same destination twice onto the back stack.
                                navController.navigate(destination) { launchSingleTop = true }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                        ) {
                            Text(if (isLoggedIn == true) "Go to checkout" else "Log in to checkout")
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (lines.isEmpty()) {
            Column(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Outlined.ShoppingCart,
                    title = "Your cart is empty",
                    body = "Buy oil in a returnable container and pay a small deposit, " +
                        "or refill a container you already hold and skip the deposit.",
                    actionLabel = "Browse the shop",
                    onAction = { navController.popBackStack(Routes.SHOP, inclusive = false) },
                )
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding),
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 2.dp),
                    ) {
                        Text("Cart items", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (totals.itemCount == 1) "1 item" else "${totals.itemCount} items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(lines, key = { "${it.productId}-${it.isRefill}" }) { line ->
                    CartLineCard(
                        line = line,
                        heldForProduct = caps[line.productId] ?: 0,
                        refillInCart = lines
                            .filter { it.productId == line.productId && it.isRefill }
                            .sumOf { it.quantity },
                        isLoggedIn = isLoggedIn == true,
                        onQuantity = { vm.setQuantity(line, it) },
                        onRefillChange = { vm.setRefill(line, it) },
                    )
                }
                item {
                    PriceDetailsCard(
                        totals = totals,
                        expanded = priceDetailsExpanded,
                        onToggle = { priceDetailsExpanded = !priceDetailsExpanded },
                        reducedMotion = reducedMotion,
                    )
                }
                if (depositSaved > 0) {
                    item {
                        DepositSavingsBanner(amount = depositSaved)
                    }
                }
            }
        }
    }
}

/** A tappable summary row that expands into the real subtotal / deposit / total breakdown. */
@Composable
private fun PriceDetailsCard(
    totals: CartTotals,
    expanded: Boolean,
    onToggle: () -> Unit,
    reducedMotion: Boolean,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(200, easing = FastOutLinearInEasing),
        label = "priceDetailsChevron",
    )

    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .animateContentSize(
                    animationSpec = if (reducedMotion) snap() else tween(220, easing = LinearOutSlowInEasing),
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Price details", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text(formatRupees(totals.payable), style = MaterialTheme.typography.titleSmall)
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse price details" else "Expand price details",
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                )
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                PriceDetailsRow("Item subtotal", formatRupees(totals.subtotal))
                if (totals.depositTotal > 0) {
                    Spacer(Modifier.height(8.dp))
                    PriceDetailsRow(
                        "Refundable container deposits",
                        formatRupees(totals.depositTotal),
                        valueColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                PriceDetailsRow(
                    "Total payable",
                    formatRupees(totals.payable),
                    emphasize = true,
                )
            }
        }
    }
}

@Composable
private fun PriceDetailsRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    emphasize: Boolean = false,
) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (emphasize) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (valueColor != androidx.compose.ui.graphics.Color.Unspecified) valueColor
                else if (emphasize) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Gold "money moment" banner — refilling instead of buying skips the deposit
 * entirely, so this is real rupees saved, not a promotional discount.
 */
@Composable
private fun DepositSavingsBanner(amount: Double) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            "You're saving ${formatRupees(amount)} by refilling instead of buying new containers.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    heldForProduct: Int,
    refillInCart: Int,
    isLoggedIn: Boolean,
    onQuantity: (Int) -> Unit,
    onRefillChange: (Boolean) -> Unit,
) {
    val hasContainer = line.containerType != null && line.containerCost > 0

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl(line.imageUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = line.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        line.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${formatRupees(line.price)} per ${line.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Quantity stepper
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = { onQuantity(line.quantity - 1) },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Reduce quantity")
                    }
                    Text(
                        "${line.quantity}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    FilledIconButton(
                        onClick = { onQuantity(line.quantity + 1) },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                    }
                }
            }

            if (hasContainer && isLoggedIn && heldForProduct > 0) {
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = line.isRefill,
                        onClick = { onRefillChange(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Refill (no deposit)") }
                    SegmentedButton(
                        selected = !line.isRefill,
                        onClick = { onRefillChange(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("Buy container") }
                }
            }

            when {
                line.isRefill && refillInCart > heldForProduct -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You hold $heldForProduct container(s) of this oil, but the cart has " +
                            "$refillInCart refills. Reduce the refill quantity or switch the rest to Buy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                !line.isRefill && hasContainer -> {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text(
                            "Container deposit (${line.containerType})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatRupees(line.containerCost * line.quantity),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Text(
                        "Refunded when you return the container.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                line.isRefill -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Refill: uses a container you already hold. No deposit.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            val lineTotal = line.price * line.quantity +
                if (!line.isRefill) line.containerCost * line.quantity else 0.0
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("You pay", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text(formatRupees(lineTotal), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
