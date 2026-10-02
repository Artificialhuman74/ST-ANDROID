package com.example.sanathanatattva.ui.screens.luckydraw

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.session.SessionViewModel
import kotlinx.coroutines.delay

@Composable
internal fun drawViewModel(consumerId: Long): LuckyDrawViewModel {
    val container = appContainer()
    return viewModel(key = "lucky_draw_$consumerId") {
        LuckyDrawViewModel(container.api, container.luckyDrawPrefs, createSavedStateHandle(), consumerId)
    }
}

/**
 * Keeps the round fresh while the screen is visible. A failed first load is retried
 * quickly: one dropped request against a cold backend must not leave the card empty
 * for the best part of a minute.
 */
@Composable
internal fun RefreshDrawOnResume(vm: LuckyDrawViewModel, details: Boolean = false) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, vm, details) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                vm.refresh(details)
                delay(if (vm.state.value.status == null) 5_000 else 45_000)
            }
        }
    }
}

@Composable
internal fun DrawCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun LuckyDrawCheckoutNote(session: SessionViewModel) {
    val consumer by session.consumer.collectAsState()
    val id = consumer?.id ?: return
    val vm = drawViewModel(id)
    val state by vm.state.collectAsState()
    RefreshDrawOnResume(vm)
    val message = checkoutDrawMessage(state.status?.cycle) ?: return
    DrawCard(LUCKY_DRAW_NAME) { Text(message, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
fun LuckyDrawOrderBadge(orderNumber: String) {
    val label = luckyDrawOrderLabel(orderNumber) ?: return
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small) {
        Text("$label · Free", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium)
    }
}
