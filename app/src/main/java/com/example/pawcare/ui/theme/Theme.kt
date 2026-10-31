package com.example.pawcare.ui.theme

import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PawColors = lightColorScheme(
    primary = Color(0xFF426B57), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EEE7), onPrimaryContainer = Color(0xFF28342E),
    secondary = Color(0xFF6D594B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3C5A7), onSecondaryContainer = Color(0xFF3D2B20),
    tertiary = Color(0xFF68756D), onTertiary = Color.White,
    background = Color(0xFFFAF8F4), onBackground = Color(0xFF28342E),
    surface = Color.White, onSurface = Color(0xFF28342E),
    surfaceVariant = Color(0xFFE6EEE7), onSurfaceVariant = Color(0xFF526058),
    surfaceContainer = Color(0xFFF4F2ED), surfaceContainerLow = Color(0xFFFAF8F4),
    outline = Color(0xFF68756D), outlineVariant = Color(0xFFE5E8E1),
    error = Color(0xFFB54343), onError = Color.White,
    errorContainer = Color(0xFFFBEAE7), onErrorContainer = Color(0xFF792828),
)
@Composable
fun PawCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PawColors, typography = Typography,
        shapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp)), content = content)
}
