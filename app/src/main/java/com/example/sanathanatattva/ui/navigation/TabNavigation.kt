package com.example.sanathanatattva.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Switches bottom-dock tabs. Each tab keeps its own back stack: leaving a tab saves it,
 * coming back restores it, and there is only ever one copy of a tab on the stack.
 */
fun NavController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Jumps from inside a screen ("View orders", "Browse the shop") to the root of a tab.
 *
 * A plain `navigate(tab)` stacks the tab on top of the current tab's history. The dock
 * then saves and restores that history with the other tab still on top of it, so the
 * tab the user came from can no longer be reached. Here the screens being left are
 * dropped instead, and the target tab opens at its root.
 */
fun NavController.openTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}
