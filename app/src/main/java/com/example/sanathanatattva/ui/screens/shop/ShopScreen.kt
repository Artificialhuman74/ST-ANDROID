package com.example.sanathanatattva.ui.screens.shop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ProductionQuantityLimits
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sanathanatattva.data.cart.totals
import com.example.sanathanatattva.data.model.Product
import com.example.sanathanatattva.imageUrl
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.EmptyState
import com.example.sanathanatattva.ui.components.ErrorState
import com.example.sanathanatattva.ui.components.SkeletonBlock
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.util.formatRupees

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(navController: NavController, session: SessionViewModel) {
    val container = appContainer()
    val vm: ShopViewModel = viewModel { ShopViewModel(container) }
    val state by vm.state.collectAsState()
    val cartLines by vm.cartLines.collectAsState()
    val isLoggedIn by session.isLoggedIn.collectAsState()

    val totals = cartLines.totals()

    var playedEntrance by remember { mutableStateOf(false) }
    LaunchedEffect(state.products.isNotEmpty()) {
        if (state.products.isNotEmpty() && !playedEntrance) {
            val staggerEnd = (state.products.size.coerceAtMost(7) * 50L) + 300L
            kotlinx.coroutines.delay(staggerEnd)
            playedEntrance = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sanathana Tattva") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.CART) }) {
                        BadgedBox(
                            badge = {
                                if (totals.itemCount > 0) Badge { Text("${totals.itemCount}") }
                            }
                        ) {
                            Icon(Icons.Outlined.ShoppingCart, contentDescription = "View cart")
                        }
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = totals.itemCount > 0,
                enter = slideInVertically(
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    initialOffsetY = { it },
                ) + fadeIn(tween(220)),
                exit = slideOutVertically(
                    animationSpec = tween(200, easing = FastOutLinearInEasing),
                    targetOffsetY = { it },
                ) + fadeOut(tween(140)),
            ) {
                CartBar(
                    itemCount = totals.itemCount,
                    amount = totals.payable,
                    onClick = { navController.navigate(Routes.CART) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            OutlinedTextField(
                value = state.search,
                onValueChange = vm::setSearch,
                placeholder = { Text("Search oils, ghee…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.submitSearch() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (state.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { vm.selectCategory(null) },
                            label = { Text("All") },
                        )
                    }
                    items(state.categories) { category ->
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = { vm.selectCategory(category) },
                            label = { Text(category.replaceFirstChar(Char::uppercase)) },
                        )
                    }
                }
            }

            when {
                state.loading -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(4) {
                        SkeletonBlock(Modifier.fillMaxWidth(), height = 120.dp, corner = 16.dp)
                    }
                }

                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = vm::load,
                )

                state.products.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.ProductionQuantityLimits,
                    title = "Nothing matches",
                    body = "Try a different search word or category.",
                )

                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(state.products, key = { _, p -> p.id }) { index, product ->
                        val staggerDelay = if (!playedEntrance) (index * 50).coerceAtMost(300) else 0
                        var visible by remember(product.id) { mutableStateOf(playedEntrance) }
                        LaunchedEffect(product.id) {
                            if (!playedEntrance) {
                                kotlinx.coroutines.delay(staggerDelay.toLong())
                                visible = true
                            }
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(280)) + slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                            ),
                        ) {
                            ProductCard(
                                product = product,
                                heldCount = state.refillCaps[product.id] ?: 0,
                                refillRemaining = vm.refillRemaining(product.id),
                                inCartCount = cartLines
                                    .filter { it.productId == product.id }
                                    .sumOf { it.quantity },
                                isLoggedIn = isLoggedIn == true,
                                onOpen = { navController.navigate(Routes.product(product.id)) },
                                onAdd = { refill -> vm.addToCart(product, refill) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    heldCount: Int,
    refillRemaining: Int,
    inCartCount: Int,
    isLoggedIn: Boolean,
    onOpen: () -> Unit,
    onAdd: (isRefill: Boolean) -> Unit,
) {
    val outOfStock = product.stock <= 0
    val context = LocalContext.current

    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(12.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl(product.image_url))
                    .crossfade(true)
                    .build(),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "per ${product.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatRupees(product.price),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "incl. GST",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val deposit = product.container_cost
                if (deposit != null && deposit > 0.0) {
                    val depositNote = if (heldCount > 0)
                        "+ ${formatRupees(deposit)} deposit · Refill skips deposit"
                    else
                        "+ ${formatRupees(deposit)} container deposit"
                    Text(
                        depositNote,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                if (isLoggedIn && heldCount > 0) {
                    Text(
                        "You hold $heldCount × ${product.container_type ?: "container"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(8.dp))

                when {
                    outOfStock -> Text(
                        "Out of stock. Check back soon.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    refillRemaining > 0 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onAdd(true) }) { Text("Refill") }
                        FilledTonalButton(onClick = { onAdd(false) }) { Text("Buy") }
                    }

                    else -> Button(onClick = { onAdd(false) }) { Text("Add") }
                }

                if (inCartCount > 0) {
                    Text(
                        "$inCartCount in cart",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun CartBar(itemCount: Int, amount: Double, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (itemCount == 1) "1 item · ${formatRupees(amount)}"
                else "$itemCount items · ${formatRupees(amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "View cart",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
