package com.masasaatim.presentation

// Android sistem, tasarım düzenleri, hazır ikonlar ve Jetpack Compose UI bileşenleri içe aktarılıyor.
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LocationOn
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
import java.util.Locale

/**
 * MainScreen: Masa saatinizin yatay modda (Landscape) çalışan ana arayüz bileşenidir.
 * ViewModel katmanındaki StateFlow akışlarını canlı dinler ve zaman ilerledikçe,
 * ya da vakitler değiştikçe sadece ilgili alanları otomatik olarak yeniden çizer.
 */
@Composable
fun MainScreen() {
    // Uygulama bağlamı (Application Context) güvenli bir şekilde çekiliyor
    val context = LocalContext.current.applicationContext as Application

    // ViewModel nesnesi fabrika deseniyle (provideFactory) tek bir sefer ve güvenle başlatılıyor.
    // Bu sayede "Redeclaration: MainViewModel" hatası tamamen çözülmüştür.
    val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(context))

    // ViewModel içerisindeki reaktif durumlar (Eyaletler) arayüz için abone ediliyor
    val currentTime by mainViewModel.currentTime.collectAsState()
    val currentDate by mainViewModel.currentDate.collectAsState()
    val prayerTimes by mainViewModel.prayerTimes.collectAsState()
    val remainingTime by mainViewModel.remainingTime.collectAsState()
    val isDimmedMode by mainViewModel.isDimmedMode.collectAsState()
    val isAzanPlaying by mainViewModel.isAzanPlaying.collectAsState()

    // Geocoder motorunun ViewModel içinde hazırladığı net şehir ismi doğrudan string metin olarak çekiliyor.
    val locationName by mainViewModel.locationName.collectAsState()

    // --- OTOMATİK EKRAN KARARTMA (AUTO-DIMMING) RENK MOTORU ---
    // Saat gece moduna girdiğinde (isDimmedMode = true), gözü yormamak için renk tonlarını otomatik boğar.
    val clockColor = if (isDimmedMode) Color(0xFF444444) else Color(0xFFFFFFFF)
    val primaryDetailColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFF00E676)
    val listLabelColor = if (isDimmedMode) Color(0xFF333333) else Color(0xB3FFFFFF)
    val dividerColor = if (isDimmedMode) Color(0xFF0A0A0A) else Color(0xFF1E1E1E)

    // Alt kontrol paneli butonlarının gece ve gündüz moduna göre loşlaştırılmış dinamik renkleri
    val iconActiveColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFF00E676)
    val iconPassiveColor = if (isDimmedMode) Color(0xFF222222) else Color(0xFF555555)

    // --- AMOLED EKRAN KORUYUCU (BURN-IN PROTECTION) ---
    // Saatin OLED ekranlarda kalıcı leke yapmasını önlemek amacıyla,
    // o anki dakikaya göre sol panel dikey Column alanını mikro düzeyde kaydırır (offset).
    val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
    val offsetX = when (currentMinute % 4) {
        0 -> 6.dp; 1 -> (-6).dp; 2 -> 4.dp; else -> (-4).dp
    }
    val offsetY = when (currentMinute % 3) {
        0 -> 4.dp; 1 -> (-4).dp; else -> 2.dp
    }

    // Tüm ekranı kaplayan ve pikselleri tamamen kapatarak pil tasarrufu sağlayan saf siyah arka plan (Box)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {            // ==========================================
            // SOL PANEL: Canlı Saat, Dinamik Konum, Tarih ve Sayaç
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1.2f) // Ekranın genişlik olarak %60'ını sol panele ayırır
                    .fillMaxHeight()
                    .offset(x = offsetX, y = offsetY), // Piksel koruyucu kayma motoruna bağlandı
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- AKILLI KONUM BAŞLIĞI (Büyük Dijital Saatin Hemen Üzerindedir) ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Konum İkonu",
                        tint = primaryDetailColor, // Gece modunda otomatik loşlaşan neon yeşil renk
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = locationName.uppercase(Locale.getDefault()), // Şehir ismi tamamen büyük harfle basılır
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDimmedMode) Color(0xFF333333) else Color.Gray, // Gece göz kamaştırmayan loş filtre
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp)) // Saat ile konum paneli arasındaki estetik mikro boşluk

                // Büyük canlı dijital saat bileşeni (Format: HH:mm:ss)
                Text(
                    text = currentTime,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Light,
                    color = clockColor,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Günün güncel Türkçe tarihi (Örn: 06 Haziran 2026, Cumartesi)
                Text(
                    text = currentDate,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryDetailColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bir sonraki ezana kalan süreyi saniyelik gösteren sayaç metni
                Text(
                    text = remainingTime,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDimmedMode) Color(0xFF222222) else Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // ==========================================
            // SAĞ PANEL: 6 Temel Namaz Vakti Listesi
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(0.8f) // Ekranın kalan %40'lık genişlik alanını listeye ayırır
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 24.dp)
                    .offset(x = -offsetX, y = -offsetY), // Sol panelin tersine hareket ederek OLED korumasını dengeler
                verticalArrangement = Arrangement.Center
            ) {
                // Veri tabanından (Room) bugünün vakitleri yüklendiyse alt alta satırları çizer
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

        // ==========================================
        // MİNİMALİST KONTROL PANELİ (Sol Alt Köşeye Sabitlendi)
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp, start = 16.dp)
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Oynat Butonu: Ezan sesini ve arka plan servisini anında manuel test etmek için kullanılır
            IconButton(
                onClick = { mainViewModel.simulateAzanTrigger() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Manuel Ezan Test Oynatımı",
                    tint = if (isAzanPlaying) iconPassiveColor else iconActiveColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Durdur Butonu: Çalan ezan sesini ve Foreground servisini anında susturarak kapatır
            IconButton(
                onClick = { mainViewModel.stopAzanPlayback() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Ezanı Sustur ve Servisi Kapat",
                    tint = if (isAzanPlaying) Color.Red else iconPassiveColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * PrayerTimeRow: Namaz vakitlerini isim solda, saat sağda olacak şekilde düzenleyen
 * ve altına çok ince loş bir ayırıcı çizgi çeken yardımcı arayüz bileşenidir.
 */
@Composable
fun PrayerTimeRow(name: String, time: String, labelColor: Color, valueColor: Color, divColor: Color) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
        // Vakitlerin arasına çekilen loş çizgi
        HorizontalDivider(color = divColor, thickness = 0.7.dp)
    }
}
