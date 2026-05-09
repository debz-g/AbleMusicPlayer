package io.github.uditkarode.able.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AbleDarkColors = darkColorScheme(
    primary = Color(0xFF5E92F3),
    onPrimary = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFFBFBFB),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFBBBBBB),
    surfaceContainer = Color(0xFF252525),
    surfaceContainerHigh = Color(0xFF333333),
    surfaceTint = Color(0xFF5E92F3),
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFFBFBFB),
    outline = Color(0xFF444444),
    inverseSurface = Color(0xFFFBFBFB),
)

@Composable
fun AbleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AbleDarkColors, content = content)
}
