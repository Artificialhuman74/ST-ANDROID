package com.example.sanathanatattva.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sanathanatattva.App
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.ui.components.DockItem
import com.example.sanathanatattva.ui.components.LiquidDock
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.screens.account.AccountScreen
import com.example.sanathanatattva.ui.screens.account.AddressesScreen
import com.example.sanathanatattva.ui.screens.account.WalletScreen
import com.example.sanathanatattva.ui.screens.auth.LoginScreen
import com.example.sanathanatattva.ui.screens.auth.RegisterScreen
import com.example.sanathanatattva.ui.screens.auth.VerifyPendingScreen
import com.example.sanathanatattva.ui.screens.cart.CartScreen
import com.example.sanathanatattva.ui.screens.checkout.CheckoutScreen
import com.example.sanathanatattva.ui.screens.containers.ContainersScreen
import com.example.sanathanatattva.ui.screens.orders.OrderDetailScreen
import com.example.sanathanatattva.ui.screens.orders.OrdersScreen
import com.example.sanathanatattva.ui.screens.product.ProductDetailScreen
import com.example.sanathanatattva.ui.screens.shop.ShopScreen
import com.example.sanathanatattva.ui.session.SessionViewModel

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as App).container

private data class BottomDest(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val bottomDests = listOf(
    BottomDest(Routes.SHOP, "Shop", Icons.Outlined.Storefront, Icons.Filled.Storefront),
    BottomDest(Routes.ORDERS, "Orders", Icons.Outlined.ReceiptLong, Icons.Filled.ReceiptLong),
    BottomDest(Routes.CONTAINERS, "Containers", Icons.Outlined.Inventory2, Icons.Filled.Inventory2),
    BottomDest(Routes.ACCOUNT, "Account", Icons.Outlined.Person, Icons.Filled.Person),
)

@Composable
fun AppRoot() {
    val container = appContainer()
    val session: SessionViewModel = viewModel { SessionViewModel(container) }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomDests.map { it.route }

    Scaffold { _ ->
        // Tab peers: near-instant fade (no slide — slides imply hierarchy, tabs are peers).
        // Push destinations: light horizontal slide + fade. Exit accelerates, enter decelerates.
        val tabFadeIn = fadeIn(tween(100))
        val tabFadeOut = fadeOut(tween(100))
        val pushEnter = slideInHorizontally(
            initialOffsetX = { it / 5 },
            animationSpec = tween(260, easing = FastOutSlowInEasing),
        ) + fadeIn(tween(220))
        val pushExit = slideOutHorizontally(
            targetOffsetX = { -it / 8 },
            animationSpec = tween(200, easing = FastOutLinearInEasing),
        ) + fadeOut(tween(160))
        val popEnter = slideInHorizontally(
            initialOffsetX = { -it / 8 },
            animationSpec = tween(260, easing = FastOutSlowInEasing),
        ) + fadeIn(tween(220))
        val popExit = slideOutHorizontally(
            targetOffsetX = { it / 5 },
            animationSpec = tween(200, easing = FastOutLinearInEasing),
        ) + fadeOut(tween(160))

        // The dock floats over the content (not a reserved bar), so screens' scrolling
        // content shows through behind it. Screens add DockContentClearance at the bottom
        // so nothing stays trapped underneath.
        Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.SHOP,
            modifier = Modifier.fillMaxSize(),
            // Default = tab-switch fade; push destinations override below.
            enterTransition = { tabFadeIn },
            exitTransition = { tabFadeOut },
            popEnterTransition = { tabFadeIn },
            popExitTransition = { tabFadeOut },
        ) {
            // ── Tab destinations (inherit fast fade from NavHost) ──────────────
            composable(Routes.SHOP) { ShopScreen(navController, session) }
            composable(Routes.ORDERS) { OrdersScreen(navController, session) }
            composable(Routes.CONTAINERS) { ContainersScreen(navController, session) }
            composable(Routes.ACCOUNT) { AccountScreen(navController, session) }

            // ── Push destinations (slide in/out) ──────────────────────────────
            composable(
                Routes.PRODUCT,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit },
            ) { entry ->
                val id = entry.arguments?.getString("productId")?.toLongOrNull() ?: return@composable
                ProductDetailScreen(navController, session, id)
            }
            composable(
                Routes.CART,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit },
            ) { CartScreen(navController, session) }
            composable(
                Routes.CHECKOUT,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit },
            ) { CheckoutScreen(navController, session) }
            composable(
                Routes.ORDER_DETAIL,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit },
            ) { entry ->
                val id = entry.arguments?.getString("orderId")?.toLongOrNull() ?: return@composable
                OrderDetailScreen(navController, id)
            }
            composable(
                Routes.ADDRESSES,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit },
            ) { AddressesScreen(navController) }
            composable(
                Routes.WALLET,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit },
            ) { WalletScreen(navController) }
            composable(
                Routes.LOGIN,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit },
            ) { LoginScreen(navController, session) }
            composable(
                Routes.REGISTER,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit },
            ) { RegisterScreen(navController, session) }
            composable(
                Routes.VERIFY_PENDING,
                enterTransition = { pushEnter },
                exitTransition = { pushExit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit },
            ) { VerifyPendingScreen(navController, session) }
        }

            if (showBottomBar) {
                LiquidDock(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    items = bottomDests.map { DockItem(it.label, it.icon, it.selectedIcon) },
                    selectedIndex = bottomDests.indexOfFirst { it.route == currentRoute },
                    onSelect = { index ->
                        navController.navigate(bottomDests[index].route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}
