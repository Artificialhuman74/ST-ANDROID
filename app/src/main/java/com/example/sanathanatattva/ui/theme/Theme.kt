package com.example.sanathanatattva.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light theme: the app's origin — a bright, green-tinted world for bright kitchens.
private val LightColorScheme = lightColorScheme(
    primary = Green600,
    onPrimary = Color.White,
    primaryContainer = Green100,
    onPrimaryContainer = Green900,
    inversePrimary = Color(0xFF8FD6A4),

    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,

    tertiary = Gold700,
    onTertiary = Color.White,
    tertiaryContainer = Gold100,
    onTertiaryContainer = Gold900,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = Mist,
    onBackground = Ink,
    surface = SurfaceBright,
    onSurface = Ink,
    surfaceVariant = SurfaceDim,
    onSurfaceVariant = InkMuted,
    outline = OutlineGreen,
    outlineVariant = OutlineFaint,

    // Cards: crisp white, defined against the near-white background by a
    // hairline outline (see AppCard / SectionCard) rather than any fill tint.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F5F0),
    surfaceContainer = Color(0xFFEDF0EA),
    surfaceContainerHigh = Color(0xFFE7EAE4),
    surfaceContainerHighest = Color(0xFFE1E5DF),
)

// Dark theme: a true-black (OLED) ground. Depth comes from surface lightness
// (higher elevation = lighter), not shadow — cards lift off black as dark
// neutral tiles with a hairline edge. The green is a fresher, more vivid grass
// green so it punches on black; gold still — and only — means money.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CDE7C),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF123B22),
    onPrimaryContainer = Color(0xFF79EBA0),
    inversePrimary = Green600,

    secondary = Color(0xFFB6CCB8),
    onSecondary = Color(0xFF213528),
    secondaryContainer = Color(0xFF35473A),
    onSecondaryContainer = Color(0xFFD2E8D4),

    tertiary = Color(0xFFE8C47D),
    onTertiary = Color(0xFF3F2E08),
    tertiaryContainer = Color(0xFF574116),
    onTertiaryContainer = Color(0xFFFBE1B0),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF000000),
    onBackground = Color(0xFFECEFEA),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFECEFEA),
    surfaceVariant = Color(0xFF3A3F3A),
    onSurfaceVariant = Color(0xFFC2C9BF),
    outline = Color(0xFF8B948C),
    outlineVariant = Color(0xFF333833),

    surfaceContainerLowest = Color(0xFF121412),
    surfaceContainerLow = Color(0xFF171917),
    surfaceContainer = Color(0xFF1C1E1C),
    surfaceContainerHigh = Color(0xFF262826),
    surfaceContainerHighest = Color(0xFF313431),
)

@Composable
fun SanathanaTattvaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Keep the system-bar icons legible against whichever ground is behind them,
    // even when the OS theme flips without recreating the activity.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
