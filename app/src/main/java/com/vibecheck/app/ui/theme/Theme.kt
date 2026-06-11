package com.vibecheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletContainer,
    onPrimaryContainer = VioletDim,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    tertiary = Coral,
    surface = SurfaceLight,
    background = SurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCDBDFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = VioletDim,
    onPrimaryContainer = VioletContainer,
    secondary = Color(0xFF4DDBCB),
    onSecondary = Color(0xFF00382F),
    tertiary = Coral,
    surface = SurfaceDark,
    background = SurfaceDark,
)

@Composable
fun VibeCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
