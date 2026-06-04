package com.masasaatim

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.masasaatim.core.theme.DesktopClockTheme
import com.masasaatim.presentation.MainScreen

/**
 * Uygulamanın Tek Giriş Kapısı.
 * [ComponentActivity] kullanımı Jetpack Compose setContent için zorunludur.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Ekranın sürekli açık kalmasını sağla (Pil tasarrufu AMOLED siyah tema ile çözüldü)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Ekranı tamamen Landscape (Yatay) moduna zorla
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // 3. Tam Ekran Modu (Sistem çubuklarını tamamen gizle ve pürüzsüz yap)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            DesktopClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // AMOLED Siyah arka plan pil tasarrufu sağlar
                ) {
                    MainScreen()
                }
            }
        }
    }
}
