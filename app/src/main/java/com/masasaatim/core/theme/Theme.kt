package com.masasaatim.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E676),      // Neon Yeşil detaylar
    background = Color(0xFF000000),   // Tam Siyah (Maksimum Pil Tasarrufu)
    surface = Color(0xFF121212),      // Koyu gri paneller
    onBackground = Color(0xFFFFFFFF), // Beyaz saat/tarih metinleri
    onSurface = Color(0xB3FFFFFF)     // Flu beyaz pasif vakitler
)

@Composable
fun DesktopClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
