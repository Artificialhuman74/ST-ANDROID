package com.example.sanathanatattva.ui.screens.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.R
import com.example.sanathanatattva.data.ApiException
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.model.Consumer
import com.example.sanathanatattva.data.model.GoogleAuthRequest
import com.example.sanathanatattva.data.model.LoginRequest
import com.example.sanathanatattva.data.model.RegisterRequest
import com.example.sanathanatattva.data.model.ResendVerificationRequest
import com.example.sanathanatattva.data.model.VerifyOtpRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    /** Dealer name when a referral code validates, null otherwise. */
    val referralDealerName: String? = null,
    val referralChecking: Boolean = false,
)

sealed interface AuthOutcome {
    data class LoggedIn(val token: String, val consumer: Consumer?) : AuthOutcome
    data class NeedsVerification(val email: String) : AuthOutcome
}

class AuthViewModel(private val container: AppContainer) : ViewModel() {

    val state = MutableStateFlow(AuthUiState())
    val outcome = MutableStateFlow<AuthOutcome?>(null)

    private var referralJob: Job? = null

    fun consumeOutcome() { outcome.value = null }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            state.value = state.value.copy(error = "Enter your email and password.")
            return
        }
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            apiCall { container.api.login(LoginRequest(email.trim(), password)) }
                .onSuccess { res ->
                    state.value = state.value.copy(loading = false)
                    val token = res.token
                    if (token != null) outcome.value = AuthOutcome.LoggedIn(token, res.consumer)
                    else state.value = state.value.copy(error = "Unexpected response from the server.")
                }
                .onFailure { e ->
                    val notVerified = (e as? ApiException)?.code == 403 &&
                        e.message.contains("verif", ignoreCase = true)
                    if (notVerified) {
                        resend(email.trim())
                        outcome.value = AuthOutcome.NeedsVerification(email.trim())
                        state.value = state.value.copy(loading = false)
                    } else {
                        state.value = state.value.copy(loading = false, error = e.message)
                    }
                }
        }
    }

    fun register(name: String, email: String, password: String, phone: String, referralCode: String) {
        when {
            name.isBlank() -> { state.value = state.value.copy(error = "Enter your name."); return }
            email.isBlank() -> { state.value = state.value.copy(error = "Enter your email."); return }
            password.length < 6 -> {
                state.value = state.value.copy(error = "Password needs at least 6 characters."); return
            }
        }
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            apiCall {
                container.api.register(
                    RegisterRequest(
                        name = name.trim(),
                        email = email.trim(),
                        password = password,
                        phone = phone.trim().ifBlank { null },
                        referral_code = referralCode.trim().ifBlank { null },
                    )
                )
            }
                .onSuccess {
                    state.value = state.value.copy(loading = false)
                    outcome.value = AuthOutcome.NeedsVerification(email.trim())
                }
                .onFailure { e ->
                    state.value = state.value.copy(loading = false, error = e.message)
                }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        if (otp.length != 6) {
            state.value = state.value.copy(error = "Enter the 6-digit code from your email.")
            return
        }
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            apiCall { container.api.verifyOtp(VerifyOtpRequest(email, otp.trim())) }
                .onSuccess { res ->
                    state.value = state.value.copy(loading = false)
                    val token = res.token
                    if (token != null) outcome.value = AuthOutcome.LoggedIn(token, res.consumer)
                    else state.value = state.value.copy(error = "Unexpected response from the server.")
                }
                .onFailure { e ->
                    state.value = state.value.copy(loading = false, error = e.message)
                }
        }
    }

    fun resend(email: String) {
        viewModelScope.launch {
            apiCall { container.api.resendVerification(ResendVerificationRequest(email)) }
                .onSuccess {
                    state.value = state.value.copy(info = "Code sent. Check your email inbox and spam folder.")
                }
                .onFailure { e ->
                    state.value = state.value.copy(error = e.message)
                }
        }
    }

    /** Debounced referral-code check that shows the dealer's name when valid. */
    fun checkReferral(code: String) {
        referralJob?.cancel()
        if (code.isBlank()) {
            state.value = state.value.copy(referralDealerName = null, referralChecking = false)
            return
        }
        referralJob = viewModelScope.launch {
            state.value = state.value.copy(referralChecking = true)
            delay(450)
            apiCall { container.api.validateDealer(code.trim()) }
                .onSuccess { res ->
                    state.value = state.value.copy(
                        referralChecking = false,
                        referralDealerName = if (res.valid) res.dealerName else null,
                    )
                }
                .onFailure {
                    state.value = state.value.copy(referralChecking = false, referralDealerName = null)
                }
        }
    }

    fun clearMessages() {
        state.value = state.value.copy(error = null, info = null)
    }

    fun signInWithGoogle(context: Context) {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.google_web_client_id))
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    apiCall { container.api.googleAuth(GoogleAuthRequest(idToken)) }
                        .onSuccess { res ->
                            state.value = state.value.copy(loading = false)
                            val token = res.token
                            if (token != null) outcome.value = AuthOutcome.LoggedIn(token, res.consumer)
                            else state.value = state.value.copy(error = "Unexpected response from the server.")
                        }
                        .onFailure { e ->
                            state.value = state.value.copy(loading = false, error = e.message)
                        }
                } else {
                    state.value = state.value.copy(loading = false, error = "Unsupported credential type.")
                }
            } catch (e: GetCredentialCancellationException) {
                state.value = state.value.copy(loading = false)
            } catch (e: NoCredentialException) {
                Log.e("GoogleAuth", "NoCredentialException: ${e.message}", e)
                state.value = state.value.copy(
                    loading = false,
                    error = "No Google account found on this device. Add a Google account in Settings and try again.",
                )
            } catch (e: GetCredentialException) {
                Log.e("GoogleAuth", "${e::class.simpleName} type=${e.type} msg=${e.message}", e)
                state.value = state.value.copy(
                    loading = false,
                    error = "Google sign-in failed (${e::class.simpleName}). Check Logcat tag 'GoogleAuth' for details.",
                )
            }
        }
    }
}
