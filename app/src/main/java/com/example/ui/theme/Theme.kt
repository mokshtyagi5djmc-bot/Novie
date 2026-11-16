package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NovieDarkExecutiveScheme = darkColorScheme(
    primary = NovieMint,
    onPrimary = NovieOnMint,
    primaryContainer = NovieMintContainer,
    onPrimaryContainer = NovieOnMintContainer,
    secondary = NovieCyan,
    onSecondary = NovieOnCyan,
    secondaryContainer = NovieCyanContainer,
    onSecondaryContainer = NovieOnCyanContainer,
    tertiary = NovieAmber,
    onTertiary = NovieOnMint,
    tertiaryContainer = NovieAmberContainer,
    onTertiaryContainer = NovieOnAmberContainer,
    background = NovieSlateBg,
    onBackground = NovieTextPrimary,
    surface = NovieSurface,
    onSurface = NovieTextPrimary,
    surfaceVariant = NovieSurfaceElevated,
    onSurfaceVariant = NovieTextSecondary,
    surfaceContainerLowest = NovieSlateBg,
    surfaceContainerLow = NovieSurface,
    surfaceContainer = NovieSurfaceElevated,
    surfaceContainerHigh = NovieSurfaceHighest,
    surfaceContainerHighest = NovieSurfaceHighest,
    outline = NovieBorder,
    outlineVariant = NovieBorderSubtle,
    error = NovieRose,
    onError = NovieOnRose,
    errorContainer = NovieRoseContainer,
    onErrorContainer = NovieOnRoseContainer
)

val NovieShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NovieTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NovieDarkExecutiveScheme,
        typography = NovieTypography,
        shapes = NovieShapes,
        content = content
    )
}
