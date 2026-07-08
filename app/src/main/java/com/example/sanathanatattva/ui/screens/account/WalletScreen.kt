package com.example.sanathanatattva.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.model.LedgerEntry
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.EmptyState
import com.example.sanathanatattva.ui.components.ErrorState
import com.example.sanathanatattva.ui.components.SkeletonBlock
import com.example.sanathanatattva.util.formatRupees
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class WalletUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val balance: Double = 0.0,
    val ledger: List<LedgerEntry> = emptyList(),
)

class WalletViewModel(private val container: AppContainer) : ViewModel() {
    val state = MutableStateFlow(WalletUiState())

    fun load() {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            apiCall { container.api.storeCredit() }
                .onSuccess { res ->
                    state.value = WalletUiState(
                        loading = false,
                        balance = res.balance,
                        ledger = res.ledger,
                    )
                }
                .onFailure { e ->
                    state.value = state.value.copy(loading = false, error = e.message)
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(navController: NavController) {
    val container = appContainer()
    val vm: WalletViewModel = viewModel { WalletViewModel(container) }
    val state by vm.state.collectAsState()
    var filter by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store credit") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            when {
                state.loading -> {
                    SkeletonBlock(Modifier.fillMaxWidth(), height = 96.dp, corner = 16.dp)
                }

                state.error != null -> ErrorState(message = state.error!!, onRetry = vm::load)

                else -> {
                    // Balance card: the one place gold carries the surface.
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "Available credit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                formatRupees(state.balance),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                "Applied at checkout. Earned from container refunds and cashback.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all" to "All", "credit" to "Added", "debit" to "Spent").forEach { (key, label) ->
                            FilterChip(
                                selected = filter == key,
                                onClick = { filter = key },
                                label = { Text(label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    val entries = state.ledger.filter {
                        filter == "all" || it.type == filter ||
                            (filter == "credit" && it.type == "refund")
                    }
                    if (entries.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            title = "No credit activity yet",
                            body = "Return a container and choose store credit, and it lands here instantly.",
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(entries, key = { it.id }) { entry ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            entry.reason ?: when (entry.type) {
                                                "debit" -> "Used on an order"
                                                else -> "Credit added"
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            entry.created_at.take(10),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        (if (entry.type == "debit") "− " else "+ ") +
                                            formatRupees(entry.amount),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (entry.type == "debit")
                                            MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
