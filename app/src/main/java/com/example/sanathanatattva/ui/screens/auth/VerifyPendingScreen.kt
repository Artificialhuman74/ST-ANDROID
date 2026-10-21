package com.example.sanathanatattva.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.Haptic
import com.example.sanathanatattva.ui.components.rememberHaptics
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.session.SessionViewModel

@Composable
fun VerifyPendingScreen(navController: NavController, session: SessionViewModel) {
    val container = appContainer()
    val vm: AuthViewModel = viewModel { AuthViewModel(container) }
    val state by vm.state.collectAsState()
    val outcome by vm.outcome.collectAsState()
    val email by session.pendingVerificationEmail.collectAsState()

    var otp by remember { mutableStateOf("") }
    val buzz = rememberHaptics()

    // Wrong or expired code: a double buzz with the message.
    LaunchedEffect(state.error) {
        if (state.error != null) buzz(Haptic.Reject)
    }

    LaunchedEffect(outcome) {
        when (val o = outcome) {
            is AuthOutcome.LoggedIn -> {
                buzz(Haptic.Confirm)
                session.onLoggedIn(o.token, o.consumer)
                session.pendingVerificationEmail.value = null
                vm.consumeOutcome()
                navController.popBackStack(Routes.SHOP, inclusive = false)
            }
            else -> {}
        }
    }

    val targetEmail = email
    if (targetEmail == null) {
        // Deep-linked here without a pending email; send them to login.
        LaunchedEffect(Unit) { navController.navigate(Routes.LOGIN) { popUpTo(Routes.SHOP) } }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.MarkEmailRead,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text("Check your email", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "We sent a 6-digit code to $targetEmail. Enter it below to finish setting up your account.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) otp = it.filter(Char::isDigit); vm.clearMessages() },
            label = { Text("6-digit code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        state.info?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                buzz(Haptic.Tick)
                vm.verifyOtp(targetEmail, otp)
            },
            enabled = !state.loading && otp.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            if (state.loading) CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            else Text("Verify email")
        }
        TextButton(onClick = { vm.resend(targetEmail) }) {
            Text("Send a new code")
        }
    }
}
