package com.masasaatim.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * MinimalScreen: Kullanıcı ayarlardan alternatif tasarımı seçtiğinde devreye giren,
 * sadece saate ve sıradaki vakit sayacına odaklanan minimalist arayüz şablonudur.
 */
@Composable
fun MinimalScreen(mainViewModel: MainViewModel) {
    // ViewModel üzerindeki canlı veriler arayüzün tetiklenmesi için State (Eyalet) olarak toplanıyor
    val minimalTime by mainViewModel.minimalTime.collectAsState() // Saniyesiz saat bilgisi (Örn: "14:45")
    val currentDate by mainViewModel.currentDate.collectAsState() // Detaylı tarih metni (Örn: "09 Haziran 2026, Salı")
    val nextVakitName by mainViewModel.nextVakitName.collectAsState() // Sıradaki namaz vaktinin Türkçe adı (Örn: "Akşam")
    val remainingTime by mainViewModel.remainingTime.collectAsState() // Sıradaki vakte kalan süre geri sayımı (Örn: "01:24:05")
    val isDimmedMode by mainViewModel.isDimmedMode.collectAsState() // Gece/Kısık ekran modu aktif mi?
    val isAzanPlaying by mainViewModel.isAzanPlaying.collectAsState() // Şu an ezan okunuyor mu?
    val locationName by mainViewModel.locationName.collectAsState() // Seçili şehir/konum adı

    // --- AKILLI GECE/GÜNDÜZ RENK MOTORU ---
    // Eğer 'isDimmedMode' true ise (Gece saatlerinde) piksellerin ışığı gözü almaması için loş/gri tonlara çekilir.
    val clockColor = if (isDimmedMode) Color(0xFF444444) else Color(0xFFFFFFFF) // Saat rengi: Koyu gri veya Saf Beyaz
    val detailColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFFCDDC39) // Vurgu rengi: Koyu yeşil veya Canlı fıstık yeşili
    val labelColor = if (isDimmedMode) Color(0xFF222222) else Color.Gray

    // Kontrol butonlarının (Oynat/Durdur) gece moduna uyumlu dinamik renk tanımlamaları
    val iconActiveColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFFCDDC39)
    val iconPassiveColor = if (isDimmedMode) Color(0xFF222222) else Color(0xFF555555)

    // Tüm ekranı kaplayan ve AMOLED ekranlar için şarj tasarrufu sağlayan saf siyah ana kutu (Container)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(24.dp)
    ) {
        // Yatay yerleşim düzeni (Sol panel saati, sağ panel sayaçları ve tarih bilgilerini tutar)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically // Elemanları dikey eksende tam ortalar
        ) {
            // =======================================================
            // SOL PANEL (%80 Genişlik): Devasa Dijital Saat & Konum İsmi
            // =======================================================
            Column(
                modifier = Modifier
                    .weight(0.8f) // Ekran genişliğinin %80'ini bu panele ayırır
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally // Elemanları yatayda ortalar
            ) {
                // Saniyesiz, devasa boyutlu dijital saat metni (180sp)
                Text(
                    text = minimalTime,
                    fontSize = 180.sp,
                    fontWeight = FontWeight.Bold,
                    color = clockColor, // Gece durumuna göre loşlaşan dinamik renk
                    letterSpacing = (-2).sp, // Sayıların birbirine daha estetik yakın durması için harf arası daraltma
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Saatin hemen altındaki yerleşim yeri (Konum) paneli
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Konum",
                        tint = detailColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Büyük harflere çevrilmiş şehir adı (Örn: ANKARA)
                    Text(
                        text = locationName.uppercase(Locale.getDefault()),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDimmedMode) Color(0xFF333333) else Color.LightGray,
                        letterSpacing = 1.sp
                    )
                }
            }

            // =======================================================
            // SAĞ PANEL (%20 Genişlik): Dinamik Tarih ve Kalan Süre Sayaçları
            // =======================================================
            Column(
                modifier = Modifier
                    .weight(0.2f) // Ekran genişliğinin kalan %20'lik dilimini kullanır
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween, // Üstteki tarih grubu ile alttaki sayaç grubunu iki zıt uca iter
                horizontalAlignment = Alignment.End // Tüm metinleri sağa yaslar
            ) {
                // SAĞ ÜST GRUP: Ayrıştırılmış Tarih ve Gün İsmi
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    // ViewModel'den gelen "09 Haziran 2026, Salı" metnini virgülden (,) ikiye böler
                    val dateParts = currentDate.split(",")
                    val rawDate = dateParts.firstOrNull() ?: "" // Virgülden öncesi: "09 Haziran 2026"
                    val dayName = dateParts.getOrNull(1)?.trim() ?: "" // Virgülden sonrası (Boşluklar temizlenmiş): "Salı"

                    // Sadece Ay ve Günün Sayısal Değeri (Örn: 09 Haziran 2026)
                    Text(
                        text = rawDate,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = clockColor,
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Altındaki renkli gün ismi metni (Örn: Salı)
                    Text(
                        text = dayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = detailColor,
                        textAlign = TextAlign.End
                    )
                }

                // SAĞ ALT GRUP: Sıradaki Vakit ve Geri Sayım Sayacı
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Hangi vakte kalındığını gösteren bilgilendirme metni (Örn: "Akşam Vaktine")
                    Text(
                        text = "$nextVakitName Vaktine",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = labelColor,
                        textAlign = TextAlign.End
                    )
                    // Geri sayım sayacı (Örn: 01:24:05). Dikkat çekmesi için kalın (Bold) ve 20sp yapılmıştır.
                    Text(
                        text = remainingTime,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = clockColor,
                        textAlign = TextAlign.End
                    )
                }
            }
        } // Row Sonu

        // =======================================================
        // KONTROL PANELİ: Sol Alt Köşe (Play, Stop, Ana Ekran)
        // =======================================================
        // Ezan simülasyonu, susturma ve ana ekrana dönüş butonlarını yan yana yerleştiren yatay panel.
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart) // Box (ekran) içerisindeki sol alt köşeye sabitleme yapar
                .background(Color.Transparent), // Buton panelinin arka planı şeffaf tutuluyor
            horizontalArrangement = Arrangement.spacedBy(8.dp), // Butonlar arasına 8dp yatay boşluk bırakır
            verticalAlignment = Alignment.CenterVertically // İkonları dikey eksende aynı hizada ortalar
        ) {
            // 1. Manuel Ezan Test Oynatımı Butonu
            // Ezan sesini anında test etmek için ViewModel'deki simülasyon fonksiyonunu tetikler.
            IconButton(
                onClick = { mainViewModel.simulateAzanTrigger() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Ezan Test",
                    // Ezan zaten çalıyorsa buton pasif (loş) renge bürünür, çalmıyorsa aktif (canlı) renkte kalır.
                    tint = if (isAzanPlaying) iconPassiveColor else iconActiveColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 2. Ezanı Susturma / Kapatma Butonu
            // Arka planda çalmakta olan ezan sesini anında keser ve servis bildirimini kapatır.
            IconButton(
                onClick = { mainViewModel.stopAzanPlayback() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Ezanı Sustur",
                    // Ezan çalarken kullanıcının dikkatini çekmek için buton rengi Kırmızı (Red) olur.
                    tint = if (isAzanPlaying) Color.Red else iconPassiveColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 3. YENİ: Klasik Ana Ekrana Geri Dönüş Butonu
            // Dikkat: Bu buton ayarlar menüsünü açmaz! ViewModel'deki arayüz modunu (isAlternativeUi)
            // tersine çevirerek (`toggleUiMode`), kullanıcının tüm vakitlerin listelendiği Klasik Ekran'a dönmesini sağlar.
            IconButton(
                onClick = { mainViewModel.toggleUiMode() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home, // İkon olarak  (Home) seçilmiş
                    contentDescription = "Ana Ekrana Geri Dön",
                    tint = iconPassiveColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    } // MinimalScreen içindeki ana Box yapısının sonu
} // MinimalScreen fonksiyonunun sonu
