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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
                // GetSignInWithGoogleOption drives the explicit "Sign in with Google"
                // account-picker flow. Unlike GetGoogleIdOption (the One Tap bottom
                // sheet), it always surfaces the picker instead of throwing
                // NoCredentialException when there's no already-authorized account.
                val googleIdOption = GetSignInWithGoogleOption.Builder(
                    // The Firebase project's web client (generated from google-services.json),
                    // so the resulting Google token can be exchanged for a Firebase token below.
                    serverClientId = context.getString(R.string.default_web_client_id),
                ).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken

                    // The backend verifies with the Firebase Admin SDK, which expects a
                    // Firebase ID token (aud = the Firebase project), not the raw Google
                    // OAuth token. Exchange it on-device so Android shares the same
                    // consumer identity as the web app.
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = FirebaseAuth.getInstance()
                        .signInWithCredential(firebaseCredential).await()
                    val firebaseIdToken = authResult.user?.getIdToken(false)?.await()?.token
                    if (firebaseIdToken == null) {
                        state.value = state.value.copy(
                            loading = false,
                            error = "Couldn't complete Google sign-in. Please try again.",
                        )
                        return@launch
                    }
                    apiCall { container.api.googleAuth(GoogleAuthRequest(firebaseIdToken)) }
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
                // With the account-picker flow this is rare; when it does fire on a
                // device that has Google accounts, the usual cause is a signing-cert
                // (SHA-1/SHA-256) fingerprint that isn't registered on the Firebase /
                // Google Cloud OAuth client — e.g. the Play App Signing certificate.
                Log.e("GoogleAuth", "NoCredentialException: ${e.message}", e)
                state.value = state.value.copy(
                    loading = false,
                    error = "Google sign-in isn't available right now. Please log in with your email and password.",
                )
            } catch (e: GetCredentialException) {
                Log.e("GoogleAuth", "${e::class.simpleName} type=${e.type} msg=${e.message}", e)
                state.value = state.value.copy(
                    loading = false,
                    error = "Google sign-in didn't work. Please try again, or log in with your email and password.",
                )
            } catch (e: Exception) {
                // Covers the Firebase credential exchange (e.g. FirebaseAuthException).
                Log.e("GoogleAuth", "Firebase exchange failed: ${e::class.simpleName} ${e.message}", e)
                state.value = state.value.copy(
                    loading = false,
                    error = "Google sign-in didn't work. Please try again, or log in with your email and password.",
                )
            }
        }
    }
}
