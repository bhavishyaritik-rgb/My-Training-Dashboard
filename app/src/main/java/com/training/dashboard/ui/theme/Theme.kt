package com.training.dashboard.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepBlack = Color(0xFF0A0B0E)
val CharcoalSurface = Color(0xFF14171F)
val ElevatedCard = Color(0xFF1D222E)
val ElectricBlue = Color(0xFF3B82F6)
val SubtlePurple = Color(0xFF8B5CF6)
val MintSuccess = Color(0xFF10B981)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = SubtlePurple,
    tertiary = MintSuccess,
    background = DeepBlack,
    surface = CharcoalSurface,
    surfaceVariant = ElevatedCard,
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF9CA3AF)
)

@Composable
fun MyTrainingTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
