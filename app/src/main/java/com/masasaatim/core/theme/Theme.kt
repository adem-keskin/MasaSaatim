package com.masasaatim.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.masasaatim.R

// =========================================================================
// 1. BÖLÜM: GOOGLE FONTS & YAZI KARAKTERİ TANIMLAMALARI
// =========================================================================

// Yazı tiplerini Google Play Servisleri üzerinden güvenli indiren sağlayıcı
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs // font_certs.xml referansı
)

// Masa saatine en çok yakışan modern ve net yazı tipi: Inter
val CustomFontName = GoogleFont("Inter")

// Tüm arayüz metinlerine giydirilecek ana font ailesi nesnesi
val appFontFamily = FontFamily(
    Font(
        googleFont = CustomFontName,
        fontProvider = provider
    )
)

// =========================================================================
// 2. BÖLÜM: MERKEZİ TİPOGRAFİ (TYPOGRAPHY) AYARLARI
// =========================================================================
// Sistemdeki tüm standart yazı tarzlarına otomatik olarak Inter fontunu atıyoruz.
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = appFontFamily),
    displayMedium = TextStyle(fontFamily = appFontFamily),
    displaySmall = TextStyle(fontFamily = appFontFamily),
    headlineLarge = TextStyle(fontFamily = appFontFamily),
    headlineMedium = TextStyle(fontFamily = appFontFamily),
    headlineSmall = TextStyle(fontFamily = appFontFamily),
    titleLarge = TextStyle(fontFamily = appFontFamily),
    titleMedium = TextStyle(fontFamily = appFontFamily),
    titleSmall = TextStyle(fontFamily = appFontFamily),
    bodyLarge = TextStyle(fontFamily = appFontFamily),
    bodyMedium = TextStyle(fontFamily = appFontFamily),
    bodySmall = TextStyle(fontFamily = appFontFamily),
    labelLarge = TextStyle(fontFamily = appFontFamily),
    labelMedium = TextStyle(fontFamily = appFontFamily),
    labelSmall = TextStyle(fontFamily = appFontFamily)
)

// =========================================================================
// 3. BÖLÜM: RENK PALETİ (AMOLED TAM SİYAH)
// =========================================================================
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E676),      // Ana vurgu rengi: Canlı dijital yeşil
    background = Color(0xFF000000),   // Arka plan: AMOLED tam siyah (Sıfır güç tüketimi)
    surface = Color(0xFF121212),      // Panel ve Diyalog yüzeyleri: Koyu gri
    onBackground = Color(0xFFFFFFFF), // Siyah zemin üzerindeki ana metinler: Saf beyaz
    onSurface = Color(0xB3FFFFFF)     // Kartlar üzerindeki ikincil metinler: %70 opak beyaz
)

// =========================================================================
// 4. BÖLÜM: ANA TEMA BİLEŞENİ (THEME WRAPPER)
// =========================================================================
@Composable
fun DesktopClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography, // 🌟 Yazı tipi sistemi burada merkezi olarak temaya bağlandı
        content = content
    )
}
