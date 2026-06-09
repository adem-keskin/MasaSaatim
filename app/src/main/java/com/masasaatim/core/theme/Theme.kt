package com.masasaatim.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Uygulamanın karanlık modda kullanacağı renk paleti tanımlanıyor.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E676),      // Ana renk: Canlı bir yeşil tonu (Butonlar, vurgular veya aktif durumlar için)
    background = Color(0xFF000000),   // Arka plan rengi: Tamamen siyah (Özellikle AMOLED ekranlarda güç tasarrufu sağlar)
    surface = Color(0xFF121212),      // Kartlar, diyaloglar veya paneller gibi yüzeylerin koyu gri rengi
    onBackground = Color(0xFFFFFFFF), // Arka planın üzerindeki metin ve ikonların rengi: Saf beyaz
    onSurface = Color(0xB3FFFFFF)     // Yüzeylerin üzerindeki metinlerin rengi: %70 opaklığa sahip beyaz (Daha az göz yorar)
)

/**
 * Uygulama veya belirli ekranlar için özel olarak oluşturulmuş Tema bileşeni (Theme Wrapper).
 * Bu fonksiyon, içine konulan tüm arayüz elemanlarına yukarıda tanımlanan renk paletini uygular.
 */
@Composable
fun DesktopClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme, // Malzeme tasarımına (Material 3) kendi renk şemamızı bağlıyoruz.
        content = content              // Temanın içine sarılacak olan arayüz (UI) bileşenleri.
    )
}
