package com.masasaatim.presentation

// Jetpack Compose tasarım, düzen, animasyon ve mimari bileşenleri içe aktarılıyor.
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar
import androidx.compose.material.icons.filled.LocationOn


/**
 * MainScreen: Uygulamanın ana ekran arayüzünü (UI) oluşturan Composable fonksiyondur.
 * Yatay modda (Landscape) masa üstü dijital saati ve namaz vakitlerini yan yana listeler.
 */
@Composable
fun MainScreen() {
    // Jetpack Compose içinden Application Context'e güvenli bir şekilde erişiliyor
    val context = LocalContext.current.applicationContext as Application

    // ViewModel kütüphanesi tetikleniyor ve projedeki fabrika (Factory) deseniyle ViewModel ayağa kaldırılıyor
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(context))

    // ViewModel içindeki canlı durumlar (StateFlow) dinleniyor.
    // Bu değerler her değiştiğinde (örn: saniye ilerledikçe) ekranın ilgili kısmı otomatik yeniden çizilir (Recomposition).
    val currentTime by viewModel.currentTime.collectAsState()        // Canlı Dijital Saat (Örn: "22:15")
    val currentDate by viewModel.currentDate.collectAsState()        // Günün Tarihi (Örn: "05 Haziran Cuma")
    val prayerTimes by viewModel.prayerTimes.collectAsState()        // Veri tabanından gelen namaz vakitleri nesnesi
    val remainingTime by viewModel.remainingTime.collectAsState()    // Bir sonraki vakte kalan süre (Geri sayım metni)
    val isDimmedMode by viewModel.isDimmedMode.collectAsState()      // Loş ışık / Gece modu aktif mi?
    val isAzanPlaying by viewModel.isAzanPlaying.collectAsState()    // O an ezan okunuyor mu?

    // --- AUTO-DIMMING RENK MOTORU ---
    // Eğer gece/loş mod aktifse (isDimmedMode), renkleri işlemci seviyesinde boğarak
    // ekranın karanlık odada gözü almasını engeller. Normal modda ise parlak beyaz ve yeşil tonları seçer.
    val clockColor = if (isDimmedMode) Color(0xFF444444) else Color(0xFFFFFFFF)
    val primaryDetailColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFF00E676)
    val listLabelColor = if (isDimmedMode) Color(0xFF333333) else Color(0xB3FFFFFF)
    val dividerColor = if (isDimmedMode) Color(0xFF0A0A0A) else Color(0xFF1E1E1E)

    // İkon butonlarının gece gözü almaması için loşlaştırılmış dinamik renkleri
    val iconActiveColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFF00E676)
    val iconPassiveColor = if (isDimmedMode) Color(0xFF222222) else Color(0xFF555555)

    // --- AMOLED EKRAN KORUYUCU (BURN-IN PROTECTION) ---
    // Sabit kalan dijital saatlerin OLED/AMOLED ekranlarda kalıcı leke (piksel yanması) yapmasını önlemek için,
    // o anki dakikaya göre tüm sol paneli matematiksel olarak birkaç piksel mikro düzeyde kaydırır (offset).
    val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
    val offsetX = when (currentMinute % 4) {
        0 -> 6.dp; 1 -> (-6).dp; 2 -> 4.dp; else -> (-4).dp
    }
    val offsetY = when (currentMinute % 3) {
        0 -> 4.dp; 1 -> (-4).dp; else -> 2.dp
    }

    // Tüm ekranı kaplayan ve arka planı saf siyah (AMOLED Siyahı) yapan ana kapsayıcı katman
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Sol ve sağ panelleri yan yana dizen yatay hizalama satırı
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SOL PANEL: Canlı Saat, Tarih ve Geri Sayıcı Alanı
            // Ekran koruyucudan gelen kayma değerleri (offset) buraya uygulanmıştır.
            Column(
                modifier = Modifier
                    .weight(1.2f) // Ekranın genişlik olarak %60'ını bu alana ayırır
                    .fillMaxHeight()
                    .offset(x = offsetX, y = offsetY),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Büyük dijital saat text bileşeni
                Text(
                    text = currentTime,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Light,
                    color = clockColor,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Günün canlı tarih metni
                Text(
                    text = currentDate,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryDetailColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Bir sonraki ezana kalan süreyi gösteren sayaç metni
                Text(
                    text = remainingTime,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDimmedMode) Color(0xFF222222) else Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // SAĞ PANEL: Ezan Vakitleri Listesi
            // Sol panelin tersi yönünde kaydırılarak ekrandaki dinamizm dengelenir.
            Column(
                modifier = Modifier
                    .weight(0.8f) // Ekranın kalan %40'lık genişliğini buraya ayırır
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 24.dp)
                    .offset(x = -offsetX, y = -offsetY),
                verticalArrangement = Arrangement.Center
            ) {
                // Veri tabanından vakitler başarıyla yüklendiyse alt alta 6 vakit satırını çizer
                prayerTimes?.let { times ->
                    PrayerTimeRow(name = "İmsak", time = times.imsak, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                    PrayerTimeRow(name = "Güneş", time = times.gunes, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                    PrayerTimeRow(name = "Öğle", time = times.ogle, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                    PrayerTimeRow(name = "İkindi", time = times.ikindi, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                    PrayerTimeRow(name = "Akşam", time = times.aksam, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                    PrayerTimeRow(name = "Yatsı", time = times.yatsi, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                }
            }
        }

        // --- SOL ALT KÖŞE MİNİMALİST KONTROL PANELİ ---
        // Kullanıcının ezanı manuel test etmesini ve çalan ezanı susturmasını sağlayan katman
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart) // Ekranın sol alt köşesine sabitlendi
                .padding(bottom = 16.dp, start = 16.dp)
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Oynat (Test Tetikleyici) Butonu: Ezanı manuel olarak test etmek için kullanılır
            IconButton(
                onClick = { viewModel.simulateAzanTrigger() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Test Et",
                    tint = if (isAzanPlaying) iconPassiveColor else iconActiveColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Durdur (Susturucu) Butonu: Çalan ezan servisini anında kapatır
            IconButton(
                onClick = { viewModel.stopAzanPlayback() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, // Durdurma onayı ikonu
                    contentDescription = "Durdur",
                    tint = if (isAzanPlaying) Color.Red else iconPassiveColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * PrayerTimeRow: Her bir namaz vaktini (İmsak, Öğle vb.) isim ve saat olarak
 * yan yana düzenleyen ve altına ince bir çizgi çeken yardımcı Composable bileşendir.
 */
@Composable
fun PrayerTimeRow(name: String, time: String, labelColor: Color, valueColor: Color, divColor: Color) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween, // İsim sola, saat sağa yaslanır
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = labelColor
            )
            Text(
                text = time,
            )
        }
    }
}