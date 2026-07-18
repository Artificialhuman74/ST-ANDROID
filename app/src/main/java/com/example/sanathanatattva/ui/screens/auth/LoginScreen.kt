package com.example.sanathanatattva.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sanathanatattva.R
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.session.SessionViewModel
import com.example.sanathanatattva.ui.theme.Green100
import com.example.sanathanatattva.ui.theme.Green600
import com.example.sanathanatattva.ui.theme.Green700
import com.example.sanathanatattva.ui.theme.Green900
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Onboarding / sign-in landing: a drenched brand hero, a glass method slider
// (Email | Phone), passwordless email OTP as the primary path, Google below.
// ---------------------------------------------------------------------------

private const val TAB_EMAIL = 0
private const val TAB_PHONE = 1

@Composable
fun LoginScreen(navController: NavController, session: SessionViewModel) {
    val container = appContainer()
    val vm: AuthViewModel = viewModel { AuthViewModel(container) }
    val state by vm.state.collectAsState()
    val outcome by vm.outcome.collectAsState()
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotion()

    var tab by rememberSaveable { mutableIntStateOf(TAB_EMAIL) }
    var email by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(outcome) {
        when (val o = outcome) {
            is AuthOutcome.LoggedIn -> {
                session.onLoggedIn(o.token, o.consumer)
                vm.consumeOutcome()
                navController.popBackStack()
            }
            is AuthOutcome.NeedsVerification -> {
                session.pendingVerificationEmail.value = o.email
                vm.consumeOutcome()
                navController.navigate(Routes.VERIFY_PENDING)
            }
            null -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero + glass slider overlapping its bottom edge.
        Box(Modifier.fillMaxWidth()) {
            HeroPanel()
            GlassTabSlider(
                options = listOf("Email", "Phone"),
                selected = tab,
                onSelect = {
                    if (it != tab) {
                        tab = it
                        vm.clearMessages()
                    }
                },
                reducedMotion = reducedMotion,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 28.dp)
                    .offset { IntOffset(0, 30.dp.roundToPx()) },
            )
        }
        Spacer(Modifier.height(54.dp))

        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { w -> dir * w / 5 } +
                        fadeIn(tween(240)))
                        .togetherWith(
                            slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { w -> -dir * w / 5 } +
                                fadeOut(tween(160)),
                        )
                }
            },
            label = "auth-pane",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) { page ->
            when (page) {
                TAB_EMAIL -> EmailPane(
                    vm = vm,
                    state = state,
                    email = email,
                    onEmailChange = { email = it; vm.clearMessages() },
                )
                else -> PhonePane(loading = state.loading)
            }
        }

        state.error?.let {
            Spacer(Modifier.height(14.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(Modifier.height(26.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                "or",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HorizontalDivider(Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))

        Box(Modifier.padding(horizontal = 24.dp)) {
            GoogleSignInButton(
                loading = state.loading,
                onClick = { vm.signInWithGoogle(context) },
            )
        }

        Spacer(Modifier.height(18.dp))
        TextButton(onClick = { navController.navigate(Routes.REGISTER) }) {
            Text("New here? Create an account")
        }
        TextButton(onClick = { navController.popBackStack() }) {
            Text(
                "Continue browsing without an account",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------

@Composable
private fun HeroPanel() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
    ) {
        // The brand emblem lives behind the green wash — a faint textured
        // watermark that reads as part of the hero, not a white logo card.
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        // Green wash: opaque enough to blend the emblem into the background,
        // sheer enough that its texture still whispers through.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Green700.copy(alpha = 0.90f),
                        1f to Green600.copy(alpha = 0.82f),
                    ),
                ),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 52.dp, bottom = 66.dp, start = 24.dp, end = 24.dp),
        ) {
            Text(
                "Welcome",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Sign in to reorder in seconds and track your containers.",
                style = MaterialTheme.typography.bodyLarge,
                color = Green100,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Glass slider
// ---------------------------------------------------------------------------

/**
 * A liquid-glass segmented control: translucent green glass over the hero's
 * edge, a bright thumb that springs between options.
 */
@Composable
private fun GlassTabSlider(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    val density = LocalDensity.current
    var widthPx by remember { mutableIntStateOf(0) }
    val padPx = with(density) { 5.dp.toPx() }
    val thumbW = if (widthPx > 0) (widthPx - 2f * padPx) / options.size else 0f
    val thumbX by animateFloatAsState(
        targetValue = padPx + selected * thumbW,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "glass-thumb",
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Green900.copy(alpha = 0.45f),
                spotColor = Green900.copy(alpha = 0.45f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to Green700.copy(alpha = 0.80f),
                    1f to Green600.copy(alpha = 0.68f),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.55f),
                    1f to Color.White.copy(alpha = 0.10f),
                ),
                shape = shape,
            )
            .onSizeChanged { widthPx = it.width },
    ) {
        if (thumbW > 0f) {
            Box(
                Modifier
                    .offset { IntOffset(thumbX.roundToInt(), padPx.roundToInt()) }
                    .size(width = with(density) { thumbW.toDp() }, height = 50.dp)
                    .shadow(4.dp, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White,
                            1f to Color(0xFFEFF3EC),
                        ),
                    ),
            )
        }
        Row(Modifier.matchParentSize()) {
            options.forEachIndexed { i, label ->
                val labelColor by animateColorAsState(
                    targetValue = if (i == selected) Green700 else Color.White,
                    animationSpec = if (reducedMotion) snap() else tween(200),
                    label = "glass-label",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Email pane: OTP-first, password fallback
// ---------------------------------------------------------------------------

@Composable
private fun EmailPane(
    vm: AuthViewModel,
    state: AuthUiState,
    email: String,
    onEmailChange: (String) -> Unit,
) {
    var usePassword by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var otp by rememberSaveable { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.emailOtpSent) {
            // --- OTP entry stage ---
            Text(
                "Enter the code",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "We emailed a 6-digit code to $email.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            OtpInput(
                value = otp,
                onValueChange = { otp = it; vm.clearMessages() },
                enabled = !state.loading,
            )
            LaunchedEffect(otp) {
                if (otp.length == 6) vm.verifyEmailLoginOtp(email, otp)
            }
            if (state.loading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            state.info?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
            ResendCountdown(
                loading = state.loading,
                onResend = { vm.sendEmailOtp(email) },
            )
            TextButton(onClick = { otp = ""; vm.resetEmailOtp() }) {
                Text("Use a different email")
            }
        } else if (!usePassword) {
            // --- OTP request stage ---
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                enabled = !state.loading,
                shape = RoundedCornerShape(16.dp),
                colors = softFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            PrimaryActionButton(
                text = "Email me a code",
                loading = state.loading,
                onClick = { vm.sendEmailOtp(email) },
            )
            TextButton(onClick = { usePassword = true; vm.clearMessages() }) {
                Text("Log in with a password instead")
            }
        } else {
            // --- Password fallback ---
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                enabled = !state.loading,
                shape = RoundedCornerShape(16.dp),
                colors = softFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; vm.clearMessages() },
                label = { Text("Password") },
                singleLine = true,
                enabled = !state.loading,
                shape = RoundedCornerShape(16.dp),
                colors = softFieldColors(),
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            PrimaryActionButton(
                text = "Log in",
                loading = state.loading,
                onClick = { vm.login(email, password) },
            )
            TextButton(onClick = { usePassword = false; vm.clearMessages() }) {
                Text("Email me a code instead")
            }
        }
    }
}

/** Resend link with a 30-second cooldown. */
@Composable
private fun ResendCountdown(loading: Boolean, onResend: () -> Unit) {
    var resendKey by rememberSaveable { mutableIntStateOf(0) }
    var seconds by remember(resendKey) { mutableIntStateOf(30) }
    LaunchedEffect(resendKey) {
        while (seconds > 0) {
            delay(1000)
            seconds--
        }
    }
    TextButton(
        enabled = seconds == 0 && !loading,
        onClick = { onResend(); resendKey++ },
    ) {
        Text(if (seconds > 0) "Resend code in ${seconds}s" else "Resend code")
    }
}

// ---------------------------------------------------------------------------
// Phone pane (styled and validated; backend not live yet)
// ---------------------------------------------------------------------------

@Composable
private fun PhonePane(loading: Boolean) {
    var phone by rememberSaveable { mutableStateOf("") }
    var fieldError by rememberSaveable { mutableStateOf<String?>(null) }
    var comingSoon by rememberSaveable { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = phone,
            onValueChange = { new ->
                phone = new.filter(Char::isDigit).take(10)
                fieldError = null
            },
            label = { Text("Mobile number") },
            prefix = { Text("+91 ") },
            singleLine = true,
            enabled = !loading,
            isError = fieldError != null,
            supportingText = fieldError?.let { { Text(it) } },
            shape = RoundedCornerShape(16.dp),
            colors = softFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        PrimaryActionButton(
            text = "Get OTP",
            loading = false,
            onClick = {
                if (phone.length != 10) {
                    fieldError = "Enter your 10-digit mobile number."
                } else {
                    comingSoon = true
                }
            },
        )
        AnimatedVisibility(
            visible = comingSoon,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Phone login is coming soon. Use email or Google to sign in today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun PrimaryActionButton(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun softFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

/** Six-box one-time-code entry backed by a single invisible text field. */
@Composable
private fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter(Char::isDigit).take(6)) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Box {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { i ->
                        val active = focused && i == value.length.coerceAtMost(5)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(width = 46.dp, height = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(14.dp),
                                ),
                        ) {
                            Text(
                                value.getOrNull(i)?.toString() ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                // The real field stays invisible; the boxes above render its value.
                Box(Modifier.matchParentSize().alpha(0f)) { innerTextField() }
            }
        },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
fun GoogleSignInButton(loading: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.Unspecified,
        )
        Spacer(Modifier.width(10.dp))
        Text("Continue with Google")
    }
}
