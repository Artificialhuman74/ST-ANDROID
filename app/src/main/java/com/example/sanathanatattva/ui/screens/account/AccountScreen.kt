package com.example.sanathanatattva.ui.screens.account

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sanathanatattva.R
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.model.ChangePasswordRequest
import com.example.sanathanatattva.data.model.UpdateProfileRequest
import com.example.sanathanatattva.ui.appContainer
import com.example.sanathanatattva.ui.components.DockContentClearance
import com.example.sanathanatattva.ui.components.EaseOutQuint
import com.example.sanathanatattva.ui.components.EmptyState
import com.example.sanathanatattva.ui.components.SkeletonBlock
import com.example.sanathanatattva.ui.components.TintedIcon
import com.example.sanathanatattva.ui.components.TopSnackbarHost
import com.example.sanathanatattva.ui.components.cardBorder
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.navigation.Routes
import com.example.sanathanatattva.ui.session.SessionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SubmitState { Idle, Loading, Success }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(navController: NavController, session: SessionViewModel) {
    val container = appContainer()
    val isLoggedIn by session.isLoggedIn.collectAsState()
    val consumer by session.consumer.collectAsState()
    val reducedMotion = rememberReducedMotion()

    var editProfile by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == true && consumer == null) session.refreshProfile()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Account") }) },
    ) { padding ->
        if (isLoggedIn != true) {
            EmptyState(
                icon = Icons.Outlined.Person,
                title = "You're browsing as a guest",
                body = "Log in to see your orders, containers, store credit and saved addresses.",
                actionLabel = "Log in",
                onAction = { navController.navigate(Routes.LOGIN) },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + DockContentClearance),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Identity + expandable editor ────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
                border = cardBorder(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    ProfileIdentity(
                        name = consumer?.name,
                        email = consumer?.email,
                        phone = consumer?.phone,
                        loading = consumer == null,
                        reducedMotion = reducedMotion,
                    )

                    Spacer(Modifier.height(16.dp))
                    EditProfileToggle(
                        expanded = editProfile,
                        reducedMotion = reducedMotion,
                        onToggle = { editProfile = !editProfile },
                    )

                    AnimatedVisibility(
                        visible = editProfile,
                        enter = expandVertically(
                            animationSpec = if (reducedMotion) snap() else tween(300, easing = EaseOutQuint),
                        ) + fadeIn(if (reducedMotion) snap() else tween(220)),
                        exit = shrinkVertically(
                            animationSpec = if (reducedMotion) snap() else tween(220, easing = EaseOutQuint),
                        ) + fadeOut(if (reducedMotion) snap() else tween(140)),
                    ) {
                        // Which account controls apply depends on how the user
                        // signs in: password accounts can change their password,
                        // Google-only accounts can't (there's nothing to change).
                        val hasPassword = consumer?.has_password ?: true
                        val hasGoogle = consumer?.has_google ?: false
                        Column {
                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(20.dp))
                            PersonalDetailsSection(
                                initialName = consumer?.name.orEmpty(),
                                email = consumer?.email.orEmpty(),
                                initialPhone = consumer?.phone.orEmpty(),
                                consumerKey = consumer?.id,
                                reducedMotion = reducedMotion,
                                onSave = { name, phone ->
                                    apiCall {
                                        container.api.updateProfile(
                                            UpdateProfileRequest(name = name, phone = phone.ifBlank { null })
                                        )
                                    }.onSuccess { session.refreshProfile() }.map { }
                                },
                                onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                            )

                            if (hasGoogle) {
                                SectionDivider()
                                SignInMethodSection()
                            }

                            if (hasPassword) {
                                SectionDivider()
                                ChangePasswordSection(
                                    reducedMotion = reducedMotion,
                                    onChange = { oldPw, newPw ->
                                        apiCall {
                                            container.api.changePassword(ChangePasswordRequest(oldPw, newPw))
                                        }.map { }
                                    },
                                    onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                                )
                            }

                            SectionDivider()
                            DangerSection(reducedMotion = reducedMotion)
                        }
                    }
                }
            }

            // ── Navigation links ────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
                border = cardBorder(),
            ) {
                Column {
                    // Store credit is money → gold medallion (the one place gold lives).
                    AccountLink(
                        icon = Icons.Outlined.CardGiftcard,
                        label = "Bhagya Chakra · Lucky draw",
                        onClick = { navController.navigate(Routes.LUCKY_DRAW) },
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AccountLink(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        label = "Store credit",
                        onClick = { navController.navigate(Routes.WALLET) },
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AccountLink(
                        icon = Icons.Outlined.Place,
                        label = "Saved addresses",
                        onClick = { navController.navigate(Routes.ADDRESSES) },
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // ── Log out — a session action, given its own quiet weight ──
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
                border = cardBorder(),
            ) {
                AccountLink(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    label = "Log out",
                    onClick = { confirmLogout = true },
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

            // In-app notifications drop in from the top, not the bottom.
            TopSnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = padding.calculateTopPadding()),
            )
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("Your cart stays on this phone. You can log back in any time.") },
            confirmButton = {
                Button(onClick = {
                    confirmLogout = false
                    session.logout()
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Stay logged in") }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Identity: monogram + warm greeting. Skeletons while the profile loads.
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileIdentity(
    name: String?,
    email: String?,
    phone: String?,
    loading: Boolean,
    reducedMotion: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Crossfade(
            targetState = loading,
            animationSpec = if (reducedMotion) snap() else tween(240),
            label = "avatar",
        ) { isLoading ->
            if (isLoading) {
                SkeletonBlock(
                    modifier = Modifier.size(56.dp),
                    height = 56.dp,
                    corner = 28.dp,
                )
            } else {
                MonogramAvatar(name = name.orEmpty())
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            if (loading) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.55f), height = 22.dp)
                Spacer(Modifier.height(8.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.75f), height = 14.dp)
            } else {
                Text(
                    "Namaste",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    name.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                email?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                phone?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonogramAvatar(name: String) {
    val initials = remember(name) {
        name.trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }
    }
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun EditProfileToggle(
    expanded: Boolean,
    reducedMotion: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(300, easing = EaseOutQuint),
        label = "chevron",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggle)
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Icon(
            Icons.Outlined.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (expanded) "Close" else "Edit profile",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Editable sections
// ─────────────────────────────────────────────────────────────────────

/** Consistent breathing room + rule between editable sections. */
@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(28.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(20.dp))
}

/** Read-only note shown when a Google account is linked (both-linked or Google-only). */
@Composable
private fun SignInMethodSection() {
    SectionHeader("Sign-in method")
    Spacer(Modifier.height(12.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Google account linked", style = MaterialTheme.typography.bodyLarge)
            Text(
                "You can sign in with Google",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, danger: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = if (danger) 0.8.sp else 0.sp,
        color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun PersonalDetailsSection(
    initialName: String,
    email: String,
    initialPhone: String,
    consumerKey: Long?,
    reducedMotion: Boolean,
    onSave: suspend (name: String, phone: String) -> Result<Unit>,
    onError: (String) -> Unit,
) {
    // Reseed the editable fields once the profile loads (or the account changes).
    var name by remember(consumerKey) { mutableStateOf(initialName) }
    var phone by remember(consumerKey) { mutableStateOf(initialPhone) }
    var state by remember { mutableStateOf(SubmitState.Idle) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    SectionHeader("Personal details")
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Full name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = email,
        onValueChange = {},
        label = { Text("Email address") },
        singleLine = true,
        enabled = false,
        supportingText = { Text("Email cannot be changed") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Phone number") },
        placeholder = { Text("e.g. 9876543210") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(18.dp))
    StatefulButton(
        label = "Save changes",
        successLabel = "Saved",
        state = state,
        enabled = name.isNotBlank(),
        reducedMotion = reducedMotion,
        onClick = {
            scope.launch {
                state = SubmitState.Loading
                onSave(name.trim(), phone.trim())
                    .onSuccess {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        state = SubmitState.Success
                        delay(1300)
                        state = SubmitState.Idle
                    }
                    .onFailure {
                        state = SubmitState.Idle
                        onError(it.message ?: "Couldn't update your details")
                    }
            }
        },
    )
}

@Composable
private fun ChangePasswordSection(
    reducedMotion: Boolean,
    onChange: suspend (oldPw: String, newPw: String) -> Result<Unit>,
    onError: (String) -> Unit,
) {
    var oldPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var showOld by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(SubmitState.Idle) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    SectionHeader("Change password")
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(
        value = oldPw,
        onValueChange = { oldPw = it },
        label = { Text("Current password") },
        singleLine = true,
        visualTransformation = if (showOld) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { showOld = !showOld }) {
                Icon(
                    if (showOld) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showOld) "Hide password" else "Show password",
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = newPw,
        onValueChange = { newPw = it },
        label = { Text("New password") },
        placeholder = { Text("At least 6 characters") },
        singleLine = true,
        visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { showNew = !showNew }) {
                Icon(
                    if (showNew) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showNew) "Hide password" else "Show password",
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(18.dp))
    StatefulButton(
        label = "Change password",
        successLabel = "Password changed",
        state = state,
        enabled = true,
        reducedMotion = reducedMotion,
        onClick = {
            when {
                oldPw.isBlank() || newPw.isBlank() -> onError("Fill in both password fields")
                newPw.length < 6 -> onError("New password must be at least 6 characters")
                else -> scope.launch {
                    state = SubmitState.Loading
                    onChange(oldPw, newPw)
                        .onSuccess {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            state = SubmitState.Success
                            oldPw = ""; newPw = ""
                            delay(1300)
                            state = SubmitState.Idle
                        }
                        .onFailure {
                            state = SubmitState.Idle
                            onError(it.message ?: "Couldn't change your password")
                        }
                }
            }
        },
    )
}

// Account deletion is handled on the website (Google Play data-deletion flow);
// the app links out to it rather than duplicating the flow.
private const val DELETE_ACCOUNT_URL = "https://sanathanatattva.shop/shop/legal#delete-account"

@Composable
private fun DangerSection(reducedMotion: Boolean) {
    val uriHandler = LocalUriHandler.current
    val danger = MaterialTheme.colorScheme.error
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.98f else 1f,
        animationSpec = tween(120, easing = EaseOutQuint),
        label = "dangerPress",
    )

    SectionHeader("DANGER", danger = true)
    Spacer(Modifier.height(12.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(MaterialTheme.shapes.medium)
            .clickable(interactionSource = interaction, indication = null) {
                uriHandler.openUri(DELETE_ACCOUNT_URL)
            }
            .heightIn(min = 48.dp)
            .padding(vertical = 8.dp),
    ) {
        Icon(
            Icons.Outlined.DeleteForever,
            contentDescription = null,
            tint = danger,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Delete account", style = MaterialTheme.typography.bodyLarge, color = danger)
            Text(
                "Permanently delete your account and data on our website",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = danger,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Shared pieces
// ─────────────────────────────────────────────────────────────────────

/** Primary button that carries its own idle → loading → success feedback. */
@Composable
private fun StatefulButton(
    label: String,
    successLabel: String,
    state: SubmitState,
    enabled: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.97f else 1f,
        animationSpec = tween(120, easing = EaseOutQuint),
        label = "btnPress",
    )
    Button(
        onClick = onClick,
        enabled = enabled && state == SubmitState.Idle,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    (fadeIn(tween(200)) + scaleIn(tween(200, easing = EaseOutQuint), initialScale = 0.85f))
                        .togetherWith(fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.85f))
                }
            },
            label = "btnContent",
        ) { s ->
            when (s) {
                SubmitState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                SubmitState.Success -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(successLabel, style = MaterialTheme.typography.labelLarge)
                }
                SubmitState.Idle -> Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun AccountLink(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            TintedIcon(icon = icon, container = container, content = content)
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick),
    )
}
