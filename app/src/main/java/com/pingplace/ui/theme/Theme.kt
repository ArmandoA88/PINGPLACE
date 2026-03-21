package com.pingplace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = MeadowGreen,
    onPrimary = CreamSurface,
    secondary = Ember,
    onSecondary = CreamSurface,
    background = CreamSurface,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = Sand,
    onSurface = Slate
)

private val DarkScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF8FD1C2),
    secondary = androidx.compose.ui.graphics.Color(0xFFF3A58F),
    background = androidx.compose.ui.graphics.Color(0xFF121816),
    surface = androidx.compose.ui.graphics.Color(0xFF1A2320),
    surfaceVariant = MeadowGreenDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFF2F7F5)
)

@Composable
fun PingPlaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = PingPlaceTypography,
        content = content
    )
}
