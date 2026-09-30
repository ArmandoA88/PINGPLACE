package com.pingplace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightScheme = lightColorScheme(
    primary = MeadowGreen,
    onPrimary = CreamSurface,
    secondary = Ember,
    onSecondary = CreamSurface,
    background = CreamSurface,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = Sand,
    onBackground = Slate,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD1F6EB),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF073F35),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFEDF3F5),
    surfaceContainerLow = androidx.compose.ui.graphics.Color.White,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF50656B),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFD8E3E7),
    onSurface = Slate
)

private val DarkScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF8FD1C2),
    secondary = androidx.compose.ui.graphics.Color(0xFFF3A58F),
    background = androidx.compose.ui.graphics.Color(0xFF0C171E),
    surface = androidx.compose.ui.graphics.Color(0xFF14252D),
    surfaceVariant = MeadowGreenDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFF2F7F5),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF164D42),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFBBF5DD),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF1B3039),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF14252D),
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
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
