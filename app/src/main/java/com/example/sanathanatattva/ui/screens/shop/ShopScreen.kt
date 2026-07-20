package com.example.sanathanatattva.ui.screens.shop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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
import com.example.sanathanatattva.ui.components.DockContentClearance
import com.example.sanathanatattva.ui.components.EmptyState
import com.example.sanathanatattva.ui.components.ErrorState
import com.example.sanathanatattva.ui.components.SkeletonBlock
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.util.formatRupees
import java.util.Calendar
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(navController: NavController, session: SessionViewModel) {
    val container = appContainer()
    val vm: ShopViewModel = viewModel { ShopViewModel(container) }
    val state by vm.state.collectAsState()
    val cartLines by vm.cartLines.collectAsState()
    val isLoggedIn by session.isLoggedIn.collectAsState()
    val consumer by session.consumer.collectAsState()
    val reducedMotion = rememberReducedMotion()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val totals = cartLines.totals()

    // A warm, time-aware greeting — personalised when we know who's shopping.
    val greeting = remember(consumer?.name) {
        val firstName = consumer?.name?.trim()?.takeIf { it.isNotEmpty() }?.substringBefore(' ')
        val part = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..22 -> "Good evening"
            // Late-night wink for a cold-pressed *oil* shop.
            else -> return@remember "Burning the midnight oil?"
        }
        if (firstName != null) "$part, $firstName" else part
    }

    // The cart icon gives a quick pop each time an item lands in it.
    val cartPop = remember { Animatable(1f) }
    var lastCount by remember { mutableStateOf(totals.itemCount) }
    LaunchedEffect(totals.itemCount) {
        if (totals.itemCount > lastCount && !reducedMotion) {
            cartPop.snapTo(1.3f)
            cartPop.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
        lastCount = totals.itemCount
    }

    var playedEntrance by remember { mutableStateOf(false) }
    LaunchedEffect(state.products.isNotEmpty()) {
        if (state.products.isNotEmpty() && !playedEntrance) {
            val staggerEnd = (state.products.size.coerceAtMost(7) * 50L) + 300L
            kotlinx.coroutines.delay(staggerEnd)
            playedEntrance = true
        }
    }

    // Search + category row: collapses in lockstep with the scroll gesture itself
    // (like a real collapsing toolbar) rather than snapping open/shut at a
    // threshold — the greeting above it stays put. headerHeightPx is the row's
    // natural height, captured on first layout; headerOffsetPx is how much of
    // that height the scroll has "spent" so far, from 0 (fully open) down to
    // -headerHeightPx (fully collapsed). onPreScroll eats scroll into this
    // offset before the list ever sees it, so the header visibly eats the
    // finger's motion first — collapsing/expanding it takes exactly as much
    // scroll as its own height, in either direction, from anywhere in the list.
    // These update on every scroll frame, so they must never be read at composition
    // scope — only inside the deferred layout/graphicsLayer lambdas below, where a
    // change re-lays-out / re-draws the header without recomposing the whole screen.
    // (mutableFloatStateOf avoids autoboxing each of those frequent writes.)
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (headerHeightPx <= 0f) return Offset.Zero
                val newOffset = (headerOffsetPx + available.y).coerceIn(-headerHeightPx, 0f)
                val consumed = newOffset - headerOffsetPx
                headerOffsetPx = newOffset
                return Offset(0f, consumed)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sanathana Tattva") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.CART) }) {
                        BadgedBox(
                            modifier = Modifier.graphicsLayer {
                                scaleX = cartPop.value
                                scaleY = cartPop.value
                            },
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
    ) { padding ->
        // The bottom navigation dock floats over this content (see AppRoot), so the
        // list scrolls behind it and the cart bar is overlaid just above it.
        Box(Modifier.padding(padding).fillMaxSize()) {
        Column(Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {

            Text(
                greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            )
            Text(
                "Cold-pressed oils, kept in steel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
            )

            Column(
                modifier = Modifier
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity),
                        )
                        if (headerHeightPx != placeable.height.toFloat()) {
                            headerHeightPx = placeable.height.toFloat()
                            headerOffsetPx = headerOffsetPx.coerceIn(-headerHeightPx, 0f)
                        }
                        val visibleHeight = (headerHeightPx + headerOffsetPx)
                            .roundToInt()
                            .coerceIn(0, placeable.height)
                        layout(placeable.width, visibleHeight) {
                            placeable.placeRelative(0, headerOffsetPx.roundToInt())
                        }
                    }
                    // Fades as it shrinks so the search field's rounded outline
                    // dissolves away rather than looking guillotined by the clip.
                    // The collapse fraction is derived *inside* the layer block so the
                    // scroll-driven state reads here (draw scope) and not in composition.
                    .graphicsLayer {
                        alpha = 1f - if (headerHeightPx > 0f) {
                            (-headerOffsetPx / headerHeightPx).coerceIn(0f, 1f)
                        } else 0f
                    },
            ) {
                OutlinedTextField(
                    value = state.search,
                    onValueChange = vm::setSearch,
                    placeholder = { Text("Search oils, ghee…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
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
                        items(state.categories, key = { it }) { category ->
                            FilterChip(
                                selected = state.selectedCategory == category,
                                onClick = { vm.selectCategory(category) },
                                label = { Text(category.replaceFirstChar(Char::uppercase)) },
                            )
                        }
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
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        // Clear the floating dock, plus the cart bar when it's showing.
                        bottom = 16.dp + DockContentClearance +
                            if (totals.itemCount > 0) 72.dp else 0.dp,
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
                                buyInCart = cartLines
                                    .filter { it.productId == product.id && !it.isRefill }
                                    .sumOf { it.quantity },
                                refillInCart = cartLines
                                    .filter { it.productId == product.id && it.isRefill }
                                    .sumOf { it.quantity },
                                isLoggedIn = isLoggedIn == true,
                                onOpen = { navController.navigate(Routes.product(product.id)) },
                                onRemove = { refill -> vm.removeFromCart(product.id, refill) },
                                onAdd = { refill ->
                                    vm.addToCart(product, refill)
                                    // Refill reuses a held container — celebrate the
                                    // deposit the customer just kept (gold = money).
                                    val deposit = product.container_cost
                                    if (refill && deposit != null && deposit > 0.0) {
                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(
                                                message = refillSavingMessage(deposit),
                                                duration = SnackbarDuration.Short,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

            AnimatedVisibility(
                visible = totals.itemCount > 0,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Sit just above the floating dock rather than well clear of it.
                    .padding(bottom = DockContentClearance - 16.dp),
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

            // Gold "deposit saved" note on refill — sits above the cart bar + dock.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DockContentClearance + if (totals.itemCount > 0) 84.dp else 8.dp)
                    .padding(horizontal = 16.dp),
            ) { data ->
                Snackbar(
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Warm, varied confirmations for the deposit a refill just saved. */
private fun refillSavingMessage(deposit: Double): String {
    val templates = listOf(
        "Reusing your container — %s deposit stays with you.",
        "No new deposit. Your %s keeps working for you.",
        "Refill added — you kept your %s deposit.",
    )
    return templates.random().format(formatRupees(deposit))
}

@Composable
private fun ProductCard(
    product: Product,
    heldCount: Int,
    refillRemaining: Int,
    buyInCart: Int,
    refillInCart: Int,
    isLoggedIn: Boolean,
    onOpen: () -> Unit,
    onAdd: (isRefill: Boolean) -> Unit,
    onRemove: (isRefill: Boolean) -> Unit,
) {
    val outOfStock = product.stock <= 0
    val context = LocalContext.current

    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(12.dp)) {
            Box(
                Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl(product.image_url))
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "per ${product.unit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatRupees(product.price),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "incl. GST",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
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

                    // Refill available (or already in the cart): a control for each of
                    // refill and buy, each swapping to a stepper once it holds items.
                    refillRemaining > 0 || refillInCart > 0 -> Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AddOrStepper(
                            label = "Refill",
                            count = refillInCart,
                            canIncrement = refillRemaining > 0,
                            onIncrement = { onAdd(true) },
                            onDecrement = { onRemove(true) },
                        )
                        AddOrStepper(
                            label = "Buy",
                            count = buyInCart,
                            tonal = true,
                            onIncrement = { onAdd(false) },
                            onDecrement = { onRemove(false) },
                        )
                    }

                    else -> AddOrStepper(
                        label = "Add",
                        count = buyInCart,
                        onIncrement = { onAdd(false) },
                        onDecrement = { onRemove(false) },
                    )
                }
            }
        }
    }
}

/**
 * A soft, gooey press response for the Add/Refill controls — the app's one
 * repeat-order hero interaction. Squashes asymmetrically on press (wider,
 * shorter, like a drop of liquid under a finger), then the two axes spring
 * back on their own independent timing so the release reads as a wobble
 * rather than a snap. A faint sheen brightens with the press for a glassy
 * read. Buy's controls intentionally don't get this — they're a different,
 * plainer action. Fully skipped under reduced motion.
 */
@Composable
private fun rememberLiquidSquish(pressed: Boolean, reducedMotion: Boolean): Modifier {
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    val sheen = remember { Animatable(0f) }

    LaunchedEffect(pressed, reducedMotion) {
        if (reducedMotion) {
            scaleX.snapTo(1f)
            scaleY.snapTo(1f)
            sheen.snapTo(0f)
            return@LaunchedEffect
        }
        if (pressed) {
            launch { scaleX.animateTo(1.08f, tween(90, easing = FastOutSlowInEasing)) }
            launch { scaleY.animateTo(0.88f, tween(90, easing = FastOutSlowInEasing)) }
            launch { sheen.animateTo(0.30f, tween(90)) }
        } else {
            launch { scaleX.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f)) }
            launch { scaleY.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 340f)) }
            launch { sheen.animateTo(0f, tween(240)) }
        }
    }

    return Modifier
        .graphicsLayer {
            this.scaleX = scaleX.value
            this.scaleY = scaleY.value
        }
        .drawWithContent {
            drawContent()
            if (sheen.value > 0f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = sheen.value), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.3f),
                        radius = size.maxDimension * 0.75f,
                    ),
                )
            }
        }
}

/**
 * Shows an add button until the line holds at least one unit, then swaps to a
 * stepper. Buy keeps its fused sage pill with a plain instant swap; Add/Refill
 * (tonal = false) morph — the pill visibly divides into the two green circles
 * instead of hard-cutting from one control to the other.
 */
@Composable
private fun AddOrStepper(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    tonal: Boolean = false,
    canIncrement: Boolean = true,
) {
    if (tonal) {
        if (count <= 0) {
            FilledTonalButton(onClick = onIncrement) { Text(label) }
        } else {
            FusedStepper(
                label = label,
                count = count,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
                canIncrement = canIncrement,
            )
        }
        return
    }

    val reducedMotion = rememberReducedMotion()
    AnimatedContent(
        targetState = count > 0,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val enterExit = if (reducedMotion) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else if (targetState) {
                // The pill dissolves as the two circles pop apart with a
                // liquid overshoot — like a droplet dividing in two.
                (fadeIn(tween(170, delayMillis = 70)) +
                    scaleIn(
                        initialScale = 0.5f,
                        animationSpec = spring(dampingRatio = 0.48f, stiffness = 330f),
                    )) togetherWith
                    (fadeOut(tween(90)) +
                        scaleOut(targetScale = 0.65f, animationSpec = tween(110)))
            } else {
                // The two droplets rejoin back into a single pill.
                (fadeIn(tween(170, delayMillis = 50)) +
                    scaleIn(
                        initialScale = 0.75f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                    )) togetherWith
                    (fadeOut(tween(110)) +
                        scaleOut(targetScale = 0.6f, animationSpec = tween(140)))
            }
            enterExit.using(
                SizeTransform(clip = false) { _, _ ->
                    if (reducedMotion) snap() else spring(dampingRatio = 0.7f, stiffness = 280f)
                },
            )
        },
        label = "addRefillMorph",
    ) { hasQuantity ->
        if (hasQuantity) {
            UnfusedStepper(
                label = label,
                count = count,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
                canIncrement = canIncrement,
            )
        } else {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            Button(
                onClick = onIncrement,
                interactionSource = interactionSource,
                modifier = rememberLiquidSquish(pressed, reducedMotion),
            ) { Text(label) }
        }
    }
}

/** Buy's stepper: one sage pill — its own visual language, kept distinct from a refill. */
@Composable
private fun FusedStepper(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    canIncrement: Boolean,
) {
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

    Surface(color = containerColor, shape = RoundedCornerShape(50)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = "Remove one $label",
                    tint = contentColor,
                )
            }
            Text(
                "$count",
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            IconButton(
                onClick = onIncrement,
                enabled = canIncrement,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add one $label",
                    tint = if (canIncrement) contentColor else contentColor.copy(alpha = 0.4f),
                )
            }
        }
    }
}

/**
 * Add/Refill's stepper: two standalone green circular buttons with the count
 * as plain text between them — deliberately not one merged shape.
 */
@Composable
private fun UnfusedStepper(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    canIncrement: Boolean,
) {
    val reducedMotion = rememberReducedMotion()
    val decrementSource = remember { MutableInteractionSource() }
    val decrementPressed by decrementSource.collectIsPressedAsState()
    val incrementSource = remember { MutableInteractionSource() }
    val incrementPressed by incrementSource.collectIsPressedAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilledIconButton(
            onClick = onDecrement,
            interactionSource = decrementSource,
            modifier = Modifier
                .size(44.dp)
                .then(rememberLiquidSquish(decrementPressed, reducedMotion)),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "Remove one $label")
        }
        Text(
            "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 22.dp),
        )
        FilledIconButton(
            onClick = onIncrement,
            enabled = canIncrement,
            interactionSource = incrementSource,
            modifier = Modifier
                .size(44.dp)
                .then(rememberLiquidSquish(incrementPressed, reducedMotion)),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            ),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add one $label")
        }
    }
}

@Composable
fun CartBar(itemCount: Int, amount: Double, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        // Bolder presence: rounder, and lifted well clear of the dock so the primary
        // conversion action reads as the loudest thing on the screen.
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Count badge — a solid onPrimary chip that anchors the bar.
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$itemCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    if (itemCount == 1) "1 item in your cart" else "in your cart",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
                Text(
                    formatRupees(amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "View cart",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
