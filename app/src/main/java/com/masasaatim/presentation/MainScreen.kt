package com.masasaatim.presentation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
    val isAlternativeUi by mainViewModel.isAlternativeUi.collectAsState() // Minimalist tasarım açık mı?
    val showSettingsDialog by mainViewModel.showSettingsDialog.collectAsState() // Ayarlar paneli görünür mü?
    val currentTime by mainViewModel.currentTime.collectAsState() // Canlı saat verisi (Örn: "12:11")
    val currentDate by mainViewModel.currentDate.collectAsState() // Canlı tarih verisi (Örn: "9 Haziran Salı")
    val prayerTimes by mainViewModel.prayerTimes.collectAsState() // Ezan vakitleri listesi
    val remainingTime by mainViewModel.remainingTime.collectAsState() // Bir sonraki vakte kalan süre sayacı
    val isDimmedMode by mainViewModel.isDimmedMode.collectAsState() // Gece/Kısık ekran modu aktif mi?
    val isAzanPlaying by mainViewModel.isAzanPlaying.collectAsState() // Şu an ezan okunuyor mu?
    val locationName by mainViewModel.locationName.collectAsState() // Seçili konum adı (Örn: "Ankara")

    // ViewModel üzerindeki canlı veriler arayüzün tetiklenmesi için State (Eyalet) olarak toplanıyor
    val minimalTime by mainViewModel.minimalTime.collectAsState() // Saniyesiz saat bilgisi (Örn: "14:45")
    val nextVakitName by mainViewModel.nextVakitName.collectAsState() // Sıradaki namaz vaktinin Türkçe adı (Örn: "Akşam")





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
                    Text(
                        text = minimalTime,
                        fontSize = 190.sp,
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


            // ==========================================
            // KONTROL PANELİ: Köşeye Sabitlendi (Sol Alt)
            // ==========================================
            // Ezan testi, susturma ve ayarlar butonlarını yan yana dikey ekranın sol altına yerleştirir.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart) // Box içerisindeki sol alt köşeye hizalama yapar
                    .padding(bottom = 16.dp, start = 16.dp)
                    .background(Color.Transparent), // Arka plan şeffaf tutuluyor
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Butonlar arasında 8dp boşluk bırakır
            ) {
                // 1. Oynat Butonu (Ezan Testi)
                // Kullanıcının ezan sesinin çalışıp çalışmadığını manuel olarak test etmesini sağlar.
                IconButton(onClick = { mainViewModel.simulateAzanTrigger() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test Oynat",
                        // Eğer şu an ezan zaten okunuyorsa pasif renge bürünür, okunmuyorsa aktif/canlı renkte kalır.
                        tint = if (isAzanPlaying) iconPassiveColor else iconActiveColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Durdur Butonu (Susturma)
                // Okunmakta olan ezanı anında kesmek/susturmak için kullanılır.
                IconButton(onClick = { mainViewModel.stopAzanPlayback() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Sustur",
                        // Ezan okunduğu esnada dikkat çekmesi için Kırmızı (Red) renge bürünür.
                        tint = if (isAzanPlaying) Color.Red else iconPassiveColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 3. MERKEZİ AYARLAR DÜĞMESİ:
                // Tıklandığında ViewModel'deki ayarlar diyaloğu görünürlük bayrağını true yapar.
                IconButton(onClick = { mainViewModel.setSettingsDialogVisible(true) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Merkezi Ayarlar Menüsünü Aç",
                        tint = iconPassiveColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

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
    val isAlternativeUi by viewModel.isAlternativeUi.collectAsState()
    val inputCity = remember { mutableStateOf(config.cityName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E), // Gece gözü yormayan asil koyu gri arka plan
        title = {
            Text(
                text = "Merkezi Uygulama Ayarları",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
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
                    text = "Konum Algılama Yöntemi:",
                    fontSize = 13.sp,
                    color = Color(0xFFCDDC39),
                    fontWeight = FontWeight.Medium
                )

                Column(
                    modifier = Modifier.selectableGroup().fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFCDDC39), unselectedColor = Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Otomatik Konum (GPS)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(text = "Cihazın uydularından canlı koordinat çözümlenir.", color = Color.Gray, fontSize = 11.sp)
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
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFCDDC39), unselectedColor = Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Manuel Şehir Girişi", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(text = "Diyanet takvimini el ile yazdığınız şehre sabitler.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.DarkGray)

                // =======================================================
                // BÖLÜM 2: DİNAMİK METİN GİRİŞ ALANI / BİLGİLENDİRME
                // =======================================================
                if (!config.isAutomatic) {
                    OutlinedTextField(
                        value = inputCity.value,
                        onValueChange = { inputCity.value = it },
                        label = { Text("Şehir İsmi Yazın (Örn: Ankara)", color = Color.LightGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF252525),
                            focusedBorderColor = Color(0xFFCDDC39),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF151515), shape = MaterialTheme.shapes.small)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "📡 GPS uyduları aktiftir. Şehir yazmak için yukarıdan 'Manuel Şehir Girişi' seçeneğini işaretleyin.",
                            fontSize = 12.sp,
                            color = Color(0xFFCDDC39),
                            lineHeight = 16.sp
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.DarkGray)

                // CURRENT UI STATUS INFO: Kullanıcının o an hangi ekranı seçtiğini görmesi için küçük bilgi metni
                Text(
                    text = if (isAlternativeUi) "Şu anki Görünüm: Minimalist Gece Modu" else "Şu anki Görünüm: Klasik Detaylı Mod",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        // =======================================================
        // BÖLÜM 3: MERKEZİ AKSİYON PANELİ (ÜÇ BUTON YAN YANA)
        // =======================================================
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, // Butonları iki uca ve merkeze dengeli dağıtır
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SOL: Kapat / Vazgeç Butonu
                TextButton(onClick = onDismiss) {
                    Text("Kapat", color = Color.LightGray)
                }

                // ORTA: Tasarımı Değiştir Butonu (Canlı Önizleme Sağlar)
                Button(
                    onClick = { viewModel.toggleUiMode() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = "Tasarımı Değiştir", color = Color(0xFFCDDC39), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // SAĞ: Kaydet ve Kapat Butonu
                Button(
                    onClick = {
                        if (config.isAutomatic) {
                            onDismiss()
                        } else {
                            viewModel.updateLocationManually(inputCity.value)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCDDC39)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        // Boş bırakıyoruz çünkü alt kısımdaki tüm buton düzenini yukarıdaki Row ile kendimiz dizayn ettik
        dismissButton = null
    )
}


/**
 * PrayerTimeRow: Sağ taraftaki ezan vakitleri listesinde her bir satırı (Örn: İmsak  03:45)
 * çizen, aralarında ince bir çizgi barındıran modüler arayüz bileşenidir.
 */
@Composable
fun PrayerTimeRow(
    name: String,        // Vaktin adı (Örn: "İmsak", "Öğle")
    time: String,        // Vaktin saat değeri (Örn: "03:45", "13:12")
    labelColor: Color,   // Vakit adının yazı rengi (Gece moduna göre dinamik gelir)
    valueColor: Color,   // Saat değerinin yazı rengi (Gece moduna göre dinamik gelir)
    divColor: Color      // Satırın altındaki bölücü çizginin rengi
) {
    // Vakit bilgilerini ve altındaki çizgiyi dikey olarak hizalamak için Column kullanılır.
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth() // Satırın tüm genişliği kaplamasını sağlar
                .padding(vertical = 6.dp), // Satırlar arasında dikeyde 6dp boşluk bırakır
            horizontalArrangement = Arrangement.SpaceBetween, // Vakit adını en sola, saati ise en sağa yaslar
            verticalAlignment = Alignment.CenterVertically   // Metinleri dikey eksende aynı hizada ortalar
        ) {
            // Sol taraftaki vakit ismi (Örn: İmsak)
            Text(text = name, fontSize = 18.sp, color = labelColor)

            // Sağ taraftaki saat değeri. Dikkat çekmesi için kalın (Bold) ve biraz daha büyük (20sp) yapılmıştır.
            Text(text = time, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
        // Satırın bittiğini gösteren, Material 3 standartlarında ince bir yatay çizgi çeker.
        HorizontalDivider(color = divColor, thickness = 0.7.dp)
    }
}
