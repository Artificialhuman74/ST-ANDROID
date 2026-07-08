package com.example.sanathanatattva.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.model.Address
import com.example.sanathanatattva.data.model.SaveAddressRequest
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.EmptyState
import com.example.sanathanatattva.ui.components.ErrorState
import com.example.sanathanatattva.ui.components.SkeletonBlock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class AddressesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val addresses: List<Address> = emptyList(),
    val saving: Boolean = false,
    val saveError: String? = null,
)

class AddressesViewModel(private val container: AppContainer) : ViewModel() {
    val state = MutableStateFlow(AddressesUiState())

    fun load() {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            apiCall { container.api.addresses() }
                .onSuccess { res ->
                    state.value = state.value.copy(loading = false, addresses = res.addresses)
                }
                .onFailure { e ->
                    state.value = state.value.copy(loading = false, error = e.message)
                }
        }
    }

    fun save(existing: Address?, request: SaveAddressRequest, onDone: () -> Unit) {
        state.value = state.value.copy(saving = true, saveError = null)
        viewModelScope.launch {
            val result =
                if (existing == null) apiCall { container.api.addAddress(request) }
                else apiCall { container.api.updateAddress(existing.id, request) }
            result
                .onSuccess {
                    state.value = state.value.copy(saving = false)
                    onDone()
                    load()
                }
                .onFailure { e ->
                    state.value = state.value.copy(saving = false, saveError = e.message)
                }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            apiCall { container.api.deleteAddress(id) }
            load()
        }
    }

    fun makeDefault(id: Long) {
        viewModelScope.launch {
            apiCall { container.api.makeDefaultAddress(id) }
            load()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressesScreen(navController: NavController) {
    val container = appContainer()
    val vm: AddressesViewModel = viewModel { AddressesViewModel(container) }
    val state by vm.state.collectAsState()

    var editing by remember { mutableStateOf<Address?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Address?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved addresses") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showForm = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add address") },
            )
        },
    ) { padding ->
        when {
            state.loading -> Column(
                Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(2) { SkeletonBlock(Modifier.fillMaxWidth(), height = 100.dp, corner = 16.dp) }
            }

            state.error != null -> ErrorState(
                message = state.error!!,
                onRetry = vm::load,
                modifier = Modifier.padding(padding),
            )

            state.addresses.isEmpty() -> EmptyState(
                icon = Icons.Outlined.Place,
                title = "No saved addresses",
                body = "Save your home address once and checkout becomes a two-tap job.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding),
            ) {
                items(state.addresses, key = { it.id }) { address ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row {
                                Text(address.label, style = MaterialTheme.typography.titleSmall)
                                if (address.is_default == 1) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Default",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Text("${address.name} · ${address.phone}",
                                style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${address.address}, ${address.pincode}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row {
                                TextButton(onClick = { editing = address; showForm = true }) {
                                    Text("Edit")
                                }
                                if (address.is_default != 1) {
                                    TextButton(onClick = { vm.makeDefault(address.id) }) {
                                        Text("Make default")
                                    }
                                }
                                TextButton(onClick = { deleting = address }) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        AddressFormDialog(
            existing = editing,
            saving = state.saving,
            saveError = state.saveError,
            onSave = { request ->
                vm.save(editing, request) { showForm = false }
            },
            onDismiss = { showForm = false },
        )
    }

    deleting?.let { address ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this address?") },
            text = { Text("${address.label}: ${address.address}, ${address.pincode}") },
            confirmButton = {
                Button(onClick = {
                    vm.delete(address.id)
                    deleting = null
                }) { Text("Delete address") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Keep it") }
            },
        )
    }
}

@Composable
private fun AddressFormDialog(
    existing: Address?,
    saving: Boolean,
    saveError: String?,
    onSave: (SaveAddressRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(existing?.label ?: "Home") }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var pincode by remember { mutableStateOf(existing?.pincode.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add address" else "Edit address") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("Label (Home, Work…)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Receiver's name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Phone") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address, onValueChange = { address = it },
                    label = { Text("Full address") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it.filter(Char::isDigit).take(6) },
                    label = { Text("PIN code") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                saveError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        SaveAddressRequest(
                            label = label.trim(),
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            pincode = pincode.trim(),
                        )
                    )
                },
                enabled = !saving && label.isNotBlank() && name.isNotBlank() &&
                    phone.isNotBlank() && address.isNotBlank() && pincode.isNotBlank(),
            ) { Text(if (saving) "Saving…" else "Save address") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
