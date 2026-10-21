package com.example.sanathanatattva.ui.screens.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.TextButton
import com.example.sanathanatattva.ui.components.CartStepper
import com.example.sanathanatattva.ui.navigation.Routes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sanathanatattva.imageUrl
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.ErrorState
import com.example.sanathanatattva.ui.components.SkeletonBlock
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.util.formatRupees

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    session: SessionViewModel,
    productId: Long,
) {
    val container = appContainer()
    val vm: ProductDetailViewModel = viewModel { ProductDetailViewModel(container, productId) }
    val state by vm.state.collectAsState()
    val isLoggedIn by session.isLoggedIn.collectAsState()
    val cartLines by vm.cartLines.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.product?.name ?: "",
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
        val product = state.product
        when {
            state.loading -> Column(
                Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SkeletonBlock(Modifier.fillMaxWidth(), height = 260.dp, corner = 16.dp)
                SkeletonBlock(Modifier.fillMaxWidth(0.7f), height = 28.dp)
                SkeletonBlock(Modifier.fillMaxWidth(0.4f), height = 22.dp)
            }

            state.error != null -> ErrorState(
                message = state.error!!,
                onRetry = vm::load,
                modifier = Modifier.padding(padding),
            )

            product != null -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                val images = state.imageList
                if (images.isNotEmpty()) {
                    val pagerState = rememberPagerState { images.size }
                    HorizontalPager(state = pagerState) { page ->
                        val context = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl(images[page]))
                                .crossfade(true)
                                .build(),
                            contentDescription = "${product.name} photo ${page + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                    }
                    if (images.size > 1) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            repeat(images.size) { i ->
                                val isSelected = i == pagerState.currentPage
                                val dotSize by animateDpAsState(
                                    targetValue = if (isSelected) 10.dp else 7.dp,
                                    animationSpec = tween(200),
                                    label = "dotSize$i",
                                )
                                val dotColor by animateColorAsState(
                                    targetValue = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant,
                                    animationSpec = tween(200),
                                    label = "dotColor$i",
                                )
                                Box(
                                    Modifier
                                        .padding(3.dp)
                                        .size(dotSize)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text(product.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "per ${product.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatRupees(product.price), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "incl. 5% GST",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (product.container_type != null && (product.container_cost ?: 0.0) > 0) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            "Comes in a returnable ${product.container_type} container. " +
                                "${formatRupees(product.container_cost ?: 0.0)} deposit, " +
                                "refunded when you return it. Refills skip the deposit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                if (isLoggedIn == true && state.heldCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You hold ${state.heldCount} × ${product.container_type ?: "container"} of this oil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (product.stock <= 0) {
                    Text(
                        "Out of stock. Check back soon.",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val buyInCart = cartLines.filter { !it.isRefill }.sumOf { it.quantity }
                    val refillInCart = cartLines.filter { it.isRefill }.sumOf { it.quantity }
                    val refillRemaining = (state.heldCount - refillInCart).coerceAtLeast(0)
                    // Same controls as the shop list, already showing what's in the cart.
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (refillRemaining > 0 || refillInCart > 0) {
                            CartStepper(
                                label = "Refill",
                                count = refillInCart,
                                canIncrement = refillRemaining > 0,
                                onIncrement = { vm.addToCart(true) },
                                onDecrement = { vm.removeFromCart(true) },
                            )
                        }
                        CartStepper(
                            label = if (state.heldCount > 0) "Buy with container" else "Add to cart",
                            count = buyInCart,
                            tonal = state.heldCount > 0,
                            onIncrement = { vm.addToCart(false) },
                            onDecrement = { vm.removeFromCart(false) },
                        )
                        AnimatedVisibility(visible = buyInCart + refillInCart > 0) {
                            TextButton(onClick = { navController.navigate(Routes.CART) }) {
                                Text("In your cart: ${buyInCart + refillInCart} · View cart")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                product.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(Modifier.height(20.dp))
                    Text("About this oil", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Reviews
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reviews", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    state.averageRating?.let { avg ->
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "$avg · ${state.reviewCount} reviews",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (state.reviews.isEmpty()) {
                    Text(
                        "No reviews yet. Reviews open up once an order is delivered.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.reviews.forEach { review ->
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    review.consumer_name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Spacer(Modifier.width(6.dp))
                                repeat(review.rating.coerceIn(0, 5)) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                if (review.verified_buyer == 1) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Verified buyer",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            review.body?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
