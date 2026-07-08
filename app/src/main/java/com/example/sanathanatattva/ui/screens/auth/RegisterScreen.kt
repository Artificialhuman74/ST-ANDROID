package com.example.sanathanatattva.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.session.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, session: SessionViewModel) {
    val container = appContainer()
    val vm: AuthViewModel = viewModel { AuthViewModel(container) }
    val state by vm.state.collectAsState()
    val outcome by vm.outcome.collectAsState()

    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referral by remember { mutableStateOf("") }

    LaunchedEffect(outcome) {
        when (val o = outcome) {
            is AuthOutcome.NeedsVerification -> {
                session.pendingVerificationEmail.value = o.email
                vm.consumeOutcome()
                navController.navigate(Routes.VERIFY_PENDING) {
                    popUpTo(Routes.REGISTER) { inclusive = true }
                }
            }
            is AuthOutcome.LoggedIn -> {
                session.onLoggedIn(o.token, o.consumer)
                vm.consumeOutcome()
                navController.popBackStack(Routes.SHOP, inclusive = false)
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create account") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; vm.clearMessages() },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; vm.clearMessages() },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; vm.clearMessages() },
                label = { Text("Phone (for delivery updates)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; vm.clearMessages() },
                label = { Text("Password (6+ characters)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = referral,
                onValueChange = {
                    referral = it.uppercase()
                    vm.checkReferral(referral)
                },
                label = { Text("Referral code (optional)") },
                singleLine = true,
                supportingText = {
                    when {
                        state.referralChecking -> Text("Checking code…")
                        state.referralDealerName != null -> Text(
                            "Referred by ${state.referralDealerName}. You get the referral discount on every order.",
                            color = MaterialTheme.colorScheme.primary,
                        )
                        referral.isNotBlank() -> Text(
                            "Code not recognised yet. Double-check with your dealer.",
                        )
                        else -> Text("Have a dealer's code? Orders through them are discounted.")
                    }
                },
                trailingIcon = {
                    if (state.referralDealerName != null) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Valid referral code",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = { vm.register(name, email, password, phone, referral) },
                enabled = !state.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.loading) CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                else Text("Create account")
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            GoogleSignInButton(
                loading = state.loading,
                onClick = { vm.signInWithGoogle(context) }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
