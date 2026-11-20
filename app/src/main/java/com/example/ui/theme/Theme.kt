package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VibeDarkColorScheme = darkColorScheme(
    primary = VibeVioletBright,
    onPrimary = Color.White,
    primaryContainer = VibeVioletDeep,
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = VibeCyan,
    onSecondary = Color(0xFF001F24),
    secondaryContainer = Color(0xFF0E3947),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = VibePink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5A163B),
    onTertiaryContainer = Color(0xFFFCE7F3),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkOutline,
    error = VibeCoral,
    onError = Color.White
)

private val VibeLightColorScheme = lightColorScheme(
    primary = VibeViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = VibePink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF500724),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = Color(0xFFE11D48),
    onError = Color.White
)

val VibePlayShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun VibePlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VibeDarkColorScheme else VibeLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VibePlayTypography,
        shapes = VibePlayShapes,
        content = content
    )
}
