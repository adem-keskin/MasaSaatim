package com.masasaatim.presentation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat



/**
 * MainScreen: Masa saatinizin yatay modda (Landscape) çalışan ana arayüz bileşenidir.
 * Klasik ve Minimalist tasarım şablonları arasında geçiş köprüsü kurar.
 */
@Composable
fun MainScreen() {

    // --- SİSTEM BAR ÇUBUKLARINI İKİ EKRANDA DA TAMAMEN GİZLEME MOTORU ---
    val localContext = LocalContext.current
    val window = (localContext as? Activity)?.window
    if (window != null) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Üst bildirim (Status bar) ve alt navigasyon çubuklarını tamamen gizler (Tam Ekran Modu)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        // Kullanıcı ekranı kenardan kaydırsa bile barların geçici görünmesini ve geri kapanmasını sağlar (Immersive Mode)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    // ViewModel fabrikası (Factory) aracılığıyla MainViewModel nesnesi bağımlılıkları ile başlatılıyor
    val context = LocalContext.current.applicationContext as Application
    val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(context))

    // ViewModel içindeki StateFlow yapıları, Compose arayüzünün anlayacağı reaktif durumlara (State) dönüştürülüyor
    val showSettingsDialog by mainViewModel.showSettingsDialog.collectAsState() // Ayarlar paneli görünür mü?
    val currentDate by mainViewModel.currentDate.collectAsState() // Canlı tarih verisi (Örn: "9 Haziran Salı")
    val remainingTime by mainViewModel.remainingTime.collectAsState() // Bir sonraki vakte kalan süre sayacı
    val isDimmedMode by mainViewModel.isDimmedMode.collectAsState() // Gece/Kısık ekran modu aktif mi?
    val isAzanPlaying by mainViewModel.isAzanPlaying.collectAsState() // Şu an ezan okunuyor mu?
    val locationName by mainViewModel.locationName.collectAsState() // Seçili konum adı (Örn: "Ankara")

    // ViewModel üzerindeki canlı veriler arayüzün tetiklenmesi için State (Eyalet) olarak toplanıyor
    val minimalTime by mainViewModel.minimalTime.collectAsState() // Saniyesiz saat bilgisi (Örn: "14:45")
    val nextVakitName by mainViewModel.nextVakitName.collectAsState() // Sıradaki namaz vaktinin Türkçe adı (Örn: "Akşam"

    // --- AKILLI GECE/GÜNDÜZ RENK MOTORU ---
    // Eğer 'isDimmedMode' true ise (Gece saatlerinde) piksellerin ışığı gözü almaması için loş/gri tonlara çekilir.
    val clockColor = if (isDimmedMode) Color(0xFF444444) else Color(0xFFFFFFFF) // Saat rengi: Koyu gri veya Saf Beyaz
    val detailColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFFCDDC39) // Vurgu rengi: Koyu yeşil veya Canlı fıstık yeşili
    val labelColor = if (isDimmedMode) Color(0xFF222222) else Color.Gray

    // Kontrol butonlarının (Oynat/Durdur) gece moduna uyumlu dinamik renk tanımlamaları
    val iconActiveColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFFCDDC39)
    val iconPassiveColor = if (isDimmedMode) Color(0xFF222222) else Color(0xFF555555)

    // Eğer ayarlar paneli durumu true ise ekranda Dialog (Açılır pencere) gösterilir
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = mainViewModel,
            onDismiss = { mainViewModel.setSettingsDialogVisible(false) }
        )
    }

        // Ana ekran taşıyıcı kutusu (Arka plan her zaman saf siyah tutularak şarj tasarrufu sağlanır)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
        ) {
            // Klasik tasarımın yatay (Row) yerleşimi başlıyor
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
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
                    // SOL PANEL İÇERİSİNDEKİ SAAT BİLEŞENİ
                    Text(
                        text = minimalTime,
                        fontSize = 200.sp,
                        fontWeight = FontWeight.Bold,
                        color = clockColor, // Gece durumuna göre loşlaşan dinamik renk
                        letterSpacing = (-2).sp, // Sayıların birbirine daha estetik yakın durması için harf arası daraltma
                        lineHeight = 200.sp, // 190sp fontun dikeyde fazladan görünmez boşluk yaratmasını engeller, kutuyu tam sınırlar
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth() // Sol panelin yatayda ortalanması için genişliği kaplasın

                            .wrapContentHeight(Alignment.CenterVertically) // Saati sol panelin tam dikey ortasına yerleştirir
                    )
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
                        val dateParts = currentDate.split(",")
                        val rawDate = dateParts.firstOrNull() ?: ""
                        val dayName = dateParts.getOrNull(1)?.trim() ?: ""

                        Text(
                            text = rawDate,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = clockColor,
                            textAlign = TextAlign.End
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = dayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = detailColor,
                            textAlign = TextAlign.End
                        )
                    }

                    // SAĞ ORTA GRUP: Konum icon ve adi (Artık Ayarlar Butonu Görevinde)
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .clickable {
                                mainViewModel.setSettingsDialogVisible(true)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Merkezi Ayarlar Menüsünü Aç",
                            tint = detailColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = locationName.uppercase(Locale.getDefault()),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDimmedMode) Color(0xFF333333) else Color.LightGray,
                            letterSpacing = 1.sp
                        )
                    }

                    // SAĞ ALT GRUP: Sıradaki Vakit, KONTROL PANELİ ve Geri Sayım Sayacı
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp), // Butonlar arasında minimal boşluk
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Oynat Butonu (Ezan Testi) - İkon Boyutu Büyütüldü
                            IconButton(
                                onClick = { mainViewModel.simulateAzanTrigger() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Test Oynat",
                                    tint = if (isAzanPlaying) iconPassiveColor else iconActiveColor,
                                    modifier = Modifier.size(24.dp) // Orijinal büyük boyuta getirildi
                                )
                            }

                            // 2. Durdur Butonu (Susturma) - İkon Boyutu Büyütüldü
                            IconButton(
                                onClick = { mainViewModel.stopAzanPlayback() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Sustur",
                                    tint = if (isAzanPlaying) Color.Red else iconPassiveColor,
                                    modifier = Modifier.size(20.dp) // Orijinal büyük boyuta getirildi
                                )
                            }
                        }

                        // Hangi vakte kalındığını gösteren bilgilendirme metni (Örn: "Akşam Vaktine")
                        Text(
                            text = "$nextVakitName Vaktine",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = labelColor,
                            textAlign = TextAlign.End
                        )
                        // Geri sayım sayacı (Artık en altta)
                        Text(
                            text = remainingTime,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = clockColor,
                            textAlign = TextAlign.End
                        )
                    }

                }
            } // Row Sonu
        } // Ana Box sonu
    } // Klasik Tasarım (Else) bloğunun sonu
// MainScreen fonksiyonunun sonu

/**
 * SettingsDialog: Arayüz şablonu değiştirme, otomatik GPS ve manuel şehir girişini
 * net iki seçenek halinde sunan modern, kararlı kontrol merkezidir.
 */
@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val config by viewModel.currentConfig.collectAsState()
    val inputCity = remember { mutableStateOf(config.cityName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1F1D1D), // Daha derin, minimalist bir koyu gri
        title = {
            Text(
                text = "Uygulama Ayarları",
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 18.sp,
                letterSpacing = 0.5.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // =======================================================
                // BÖLÜM 1: KONUM YÖNTEMİ SEÇİMİ (RADYO BUTONLAR)
                // =======================================================
                Text(
                    text = "Konum Algılama Yöntemi",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )

                Column(
                    modifier = Modifier.selectableGroup().fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SEÇENEK A: OTOMATİK GPS MODU
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAutomaticLocation(true) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.isAutomatic,
                            onClick = { viewModel.toggleAutomaticLocation(true) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Otomatik Konum (GPS)", color = Color.White, fontSize = 14.sp)
                            Text(text = "Canlı koordinat çözümlenir.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    // SEÇENEK B: MANUEL ŞEHİR GİRİŞ MODU
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAutomaticLocation(false) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !config.isAutomatic,
                            onClick = { viewModel.toggleAutomaticLocation(false) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Manuel Şehir Girişi", color = Color.White, fontSize = 14.sp)
                            Text(text = "Takvimi el ile yazdığınız şehre sabitler.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF2A2A2A))

                // =======================================================
                // BÖLÜM 2: DİNAMİK METİN GİRİŞ ALANI / BİLGİLENDİRME
                // =======================================================
                if (!config.isAutomatic) {
                    OutlinedTextField(
                        value = inputCity.value,
                        onValueChange = { inputCity.value = it },
                        label = { Text("Şehir İsmi Yazın", color = Color.Gray, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1A1A1A),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0xFF2A2A2A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.small)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "📡 GPS uyduları aktiftir. Şehir değiştirmek için yukarıdan 'Manuel Şehir Girişi' seçeneğini işaretleyin.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        // =======================================================
        // BÖLÜM 3: MERKEZİ AKSİYON PANELİ (SADECE 2 BUTON)
        // =======================================================
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End, // Butonları sağa yaslar
                verticalAlignment = Alignment.CenterVertically
            ) {
                // İptal Butonu
                TextButton(onClick = onDismiss) {
                    Text("İptal", color = Color.Gray, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Kaydet Butonu
                Button(
                    onClick = {
                        if (config.isAutomatic) {
                            onDismiss()
                        } else {
                            viewModel.updateLocationManually(inputCity.value)
                            onDismiss() // Değişikliği yaptıktan sonra pencereyi kapatır
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text("Kaydet", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        },
        dismissButton = null
    )
}
