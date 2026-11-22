package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SounceDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = ObsidianElevated,
    onPrimaryContainer = NeonCyan,
    secondary = NeonViolet,
    onSecondary = TextPrimary,
    secondaryContainer = ObsidianCard,
    onSecondaryContainer = NeonViolet,
    tertiary = NeonEmerald,
    onTertiary = ObsidianBlack,
    tertiaryContainer = ObsidianElevated,
    onTertiaryContainer = NeonEmerald,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianOutline,
    error = StatusError,
    onError = TextPrimary
)

val SounceShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SounceControllerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SounceDarkColorScheme,
        typography = SounceTypography,
        shapes = SounceShapes,
        content = content
    )
}
