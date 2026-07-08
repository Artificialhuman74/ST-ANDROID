package com.example.sanathanatattva.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light-only for v1: the audience shops in bright kitchens; dark theme is deferred.
private val BrandColorScheme = lightColorScheme(
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

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F5F0),
    surfaceContainer = Color(0xFFEDF0EA),
    surfaceContainerHigh = Color(0xFFE7EAE4),
    surfaceContainerHighest = Color(0xFFE1E5DF),
)

@Composable
fun SanathanaTattvaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrandColorScheme,
        typography = Typography,
        content = content
    )
}
