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
import java.util.Calendar
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

    // Eğer ayarlar paneli durumu true ise ekranda Dialog (Açılır pencere) gösterilir
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = mainViewModel,
            onDismiss = { mainViewModel.setSettingsDialogVisible(false) }
        )
    }

    // --- GEÇİŞ KÖPRÜSÜ: Eğer Kullanıcı Alternatif / Minimalist Tasarımı Seçtiyse ---
    if (isAlternativeUi) {
        MinimalScreen(mainViewModel = mainViewModel) // Diğer ekran bileşeni çağrılır
    } else {
        // --- KLASİK TASARIM MOTORU ---

        // Ekranın gece/kısık modda (isDimmedMode) olup olmamasına göre dinamik renk paleti ayarlanıyor:
        val clockColor = if (isDimmedMode) Color(0xFF444444) else Color(0xFFFFFFFF) // Kısık modda gri, normalde beyaz saat
        val primaryDetailColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFFCDDC39) // Vurgu rengi (Yeşil / Sarı-Yeşil)
        val listLabelColor = if (isDimmedMode) Color(0xFF333333) else Color(0xB3FFFFFF)
        val dividerColor = if (isDimmedMode) Color(0xFF0A0A0A) else Color(0xFF1E1E1E)

        val iconActiveColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFFCDDC39)
        val iconPassiveColor = if (isDimmedMode) Color(0xFF222222) else Color(0xFF555555)

        // --- AMOLED EKRAN KORUMA ALGORİTMASI (BURN-IN PROTECTION) ---
        // Saat sürekli aynı piksellerde kalırsa ekranda kalıcı iz bırakır (Ekran yanması).
        // Bu algoritma, her dakika değişiminde (currentMinute % 4 ve % 3) sol paneli birkaç piksel (dp) kaydırır.
        val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
        val offsetX = when (currentMinute % 4) {
            0 -> 6.dp; 1 -> (-6).dp; 2 -> 4.dp; else -> (-4).dp
        }
        val offsetY = when (currentMinute % 3) {
            0 -> 4.dp; 1 -> (-4).dp; else -> 2.dp
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
                // SOL PANEL: Canlı Saat, Konum, Tarih ve Kalan Süre Sayacı
                // 'offset' parametresi yukarıda hesaplanan piksel kaydırma (Burn-in) değerlerini uyguluyor.
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .offset(x = offsetX, y = offsetY),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Konum Paneli: İkon ve Konum İsmi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Konum İkonu",
                            tint = primaryDetailColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = locationName.uppercase(Locale.getDefault()),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDimmedMode) Color(0xFF333333) else Color.Gray,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Dijital Saat Metni (Büyük Punto)
                    Text(
                        text = currentTime,
                        fontSize = 100.sp,
                        fontWeight = FontWeight.Light,
                        color = clockColor,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Canlı Tarih Metni
                    Text(
                        text = currentDate,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryDetailColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bir sonraki vakte kalan süreyi gösteren geri sayım metni
                    Text(
                        text = remainingTime,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isDimmedMode) Color(0xFF222222) else Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                // SAĞ PANEL: 6 Temel Namaz Vakti Listesi
                Column(
                    modifier = Modifier
                        .weight(0.8f) // Sağ panelin ekran genişliğindeki payı (%40 civarı)
                        .fillMaxHeight() // Ekranın tüm yüksekliğini kaplar
                        .padding(start = 16.dp, end = 22.dp) // Kenar boşlukları ayarlanıyor

                        // --- TERS PİKSEL KAYDIRMA ALGORİTMASI ---
                        // Sol panel +X yönüne kayarken sağ panel -X yönüne kayarak ekran genelindeki
                        // statik piksellerin (yerleşik çizgiler ve yazılar) yerini sürekli değiştirir.
                        // Bu sayede AMOLED ekran koruması tüm arayüz için tamamlanmış olur.
                        .offset(x = -offsetX, y = -offsetY),

                    verticalArrangement = Arrangement.Center // Vakit satırlarını dikeyde ortalar
                ) {
                    // Veritabanından veya API'den gelen ezan vakitleri boş (null) değilse listeleme başlar
                    prayerTimes?.let { times ->

                        // Her bir ezan vakti için özelleştirilmiş 'PrayerTimeRow' bileşeni çağrılıyor.
                        // Bu fonksiyon muhtemelen alt satırlarda tanımlı olan, solunda vakit adı (örn: İmsak),
                        // sağında ise saat (örn: 03:45) yazan ve altında bir çizgi (divider) barındıran özel bir tasarımdır.

                        PrayerTimeRow(
                            "İmsak",
                            times.imsak,
                            listLabelColor,  // Vakit adının rengi (Gece moduna göre dinamik)
                            clockColor,      // Saat değerinin rengi (Gece moduna göre dinamik)
                            dividerColor     // Altlarındaki ayırıcı çizginin rengi
                        )

                        PrayerTimeRow(
                            "Güneş",
                            times.gunes,
                            listLabelColor,
                            clockColor,
                            dividerColor
                        )

                        PrayerTimeRow("Öğle", times.ogle, listLabelColor, clockColor, dividerColor)

                        PrayerTimeRow(
                            "İkindi",
                            times.ikindi,
                            listLabelColor,
                            clockColor,
                            dividerColor
                        )

                        PrayerTimeRow(
                            "Akşam",
                            times.aksam,
                            listLabelColor,
                            clockColor,
                            dividerColor
                        )

                        PrayerTimeRow(
                            "Yatsı",
                            times.yatsi,
                            listLabelColor,
                            clockColor,
                            dividerColor
                        )
                    }
                }
            } // Row sonu

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
} // MainScreen fonksiyonunun sonu

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
