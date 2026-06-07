package com.masasaatim.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E676),
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xB3FFFFFF)
)

@Composable
fun DesktopClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
