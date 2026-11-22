package com.dairyflow.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = DairyPrimary,
    onPrimary = Color.White,
    primaryContainer = DairyPrimaryLight,
    onPrimaryContainer = DairyPrimaryDark,
    secondary = DairyPrimaryHover,
    onSecondary = Color.White,
    tertiary = DairyWarning,
    background = DairyBackground,
    onBackground = DairyTextPrimary,
    surface = DairySurface,
    onSurface = DairyTextPrimary,
    surfaceVariant = DairySurfaceVariant,
    onSurfaceVariant = DairyTextSecondary,
    outline = DairyBorder,
    error = DairyCritical,
    onError = Color.White,
    errorContainer = DairyCriticalLight,
    onErrorContainer = DairyCritical
)

val DairyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DairyFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = DairyShapes,
        content = content
    )
}
