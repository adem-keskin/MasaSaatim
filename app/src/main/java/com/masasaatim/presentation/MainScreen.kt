package com.masasaatim.presentation

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Masa Saati uygulamasının ana ekran arayüz bileşeni.
 * Durum akışlarını (StateFlow) dinleyerek ekranı dinamik olarak günceller.
 */
@Composable
fun MainScreen() {
    val localContext = LocalContext.current
    val window = (localContext as? Activity)?.window

    // Tam ekran modu ayarları (Sistem barlarını gizler, kaydırmayla görünür kılar)
    if (window != null) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    // ViewModel bağlantısı ve Durum (State) takipleri
    val mainViewModel: MainViewModel = viewModel()

    val showSettingsDialog by mainViewModel.showSettingsDialog.collectAsState()
    val remainingTime by mainViewModel.remainingTime.collectAsState()
    val isDimmedMode by mainViewModel.isDimmedMode.collectAsState()
    val isAzanPlaying by mainViewModel.isAzanPlaying.collectAsState()
    val locationName by mainViewModel.locationName.collectAsState()
    val currentTime by mainViewModel.currentTime.collectAsState()
    val currentDate by mainViewModel.currentDate.collectAsState()
    val nextVakitName by mainViewModel.nextVakitName.collectAsState()

    // Piksel koruma (Burn-in Protection) koordinatları
    val pixelOffsetX by mainViewModel.pixelOffsetX.collectAsState()
    val pixelOffsetY by mainViewModel.pixelOffsetY.collectAsState()

    // Gece modu (Loş mod) renk geçiş dinamikleri
    val clockColor = if (isDimmedMode) Color(0xFF444444) else Color(0xFFFFFFFF)
    val detailColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFFCDDC39)
    val iconActiveColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFFCDDC39)
    val iconPassiveColor = if (isDimmedMode) Color(0xFF222222) else Color(0xFF555555)

    // Ayarlar penceresi görünürlük kontrolü
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = mainViewModel,
            onDismiss = { mainViewModel.setSettingsDialogVisible(false) }
        )
    }

    // Ana Ekran AMOLED Siyah Katmanı
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // 📦 1. PARÇA KATMANI: Ana Saat ve Sağ Panel Yerleşim Kombinasyonu
        MainClockLayout(
            currentTime = currentTime,
            clockColor = clockColor,
            pixelOffsetX = pixelOffsetX,
            pixelOffsetY = pixelOffsetY,
            rightPanelContent = {

                // -------------------------------------------------------------------------
                // 1. BÖLÜM (ÜST): Sağa Yaslı Detaylı Tarih Alanı
                // -------------------------------------------------------------------------
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Türkçe yerelleştirme dil desteğini hafızaya alıyoruz
                    val trLocale = remember { Locale("tr", "TR") }
                    val numericDateFormat = remember { SimpleDateFormat("dd.MM.yyyy", trLocale) }
                    val formattedNumericDate = numericDateFormat.format(java.util.Date())

                    // ViewModel'den gelen gün ismini çözümlüyoruz
                    val dateParts = currentDate.split(",")
                    val rawDayName = dateParts.getOrNull(0)?.trim() ?: ""

                    // Gün isminin sadece ilk harfini büyüterek estetik standart sağlıyoruz
                    val formattedDayName = rawDayName.lowercase(trLocale).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(trLocale) else it.toString()
                    }

                    // Üst Satır: Sayısal Tarih (Örn: 14.07.2026)
                    Text(
                        text = formattedNumericDate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = clockColor,
                        textAlign = TextAlign.End
                    )

                    // Alt Satır: Sadece Gün İsmi Görünür (Örn: Salı)
                    Text(
                        text = formattedDayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = detailColor,
                        textAlign = TextAlign.End
                    )
                }


                // -------------------------------------------------------------------------
                // 2. BÖLÜM (ORTA): Konum İkonu ve Tek Şehir İsmi (Tıklanabilir Ayarlar)
                // -------------------------------------------------------------------------
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { mainViewModel.setSettingsDialogVisible(true) }
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ayarlar",
                        tint = detailColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = locationName.uppercase(Locale("tr")),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = clockColor,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.End
                    )
                }

                // -------------------------------------------------------------------------
                // 3. BÖLÜM (ALT): Medya Butonları, Sıradaki Vakit ve Geri Sayım Sayacı
                // -------------------------------------------------------------------------
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Üst Sıra: Ezan Sesini Test Etme ve Susturma Butonları
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { mainViewModel.simulateAzanTrigger() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Test Oynat",
                                tint = if (isAzanPlaying) iconPassiveColor else iconActiveColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(
                            onClick = { mainViewModel.stopAzanPlayback() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Sustur",
                                tint = if (isAzanPlaying) Color.Red else iconPassiveColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Alt Sıra: Ezan Vakti Adı ve Canlı Sayaç Gösterimi
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "$nextVakitName Vaktine",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.LightGray,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = remainingTime,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = clockColor,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        )
    }
} // 🌟 MainScreen() ana arayüz fonksiyonunun nihai güvenli kapanış parantezi


// =========================================================================
// 📦 ANA YERLEŞİM BİLEŞENİ (MAIN CLOCK LAYOUT)
// =========================================================================
/**
 * Ekran yerleşimini sol panel (%80 saat) ve sağ panel (%20 veriler) olarak dengeler.
 * Dikeyde tüm arayüz bileşenlerini tam merkez eksende hizalar.
 */
@Composable
fun MainClockLayout(
    currentTime: String,
    clockColor: Color,
    pixelOffsetX: Float,
    pixelOffsetY: Float,
    rightPanelContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically // Sol ve sağ paneli ekrana göre tam dikey ortalar
    ) {
        // SOL PANEL (%80 Genişlik): Devasa Dijital Saat Hücresi
        Column(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DigitalClockView(
                currentTime = currentTime,
                clockColor = clockColor,
                pixelOffsetX = pixelOffsetX,
                pixelOffsetY = pixelOffsetY
            )
        }

        // SAĞ PANEL (%20 Genişlik): Dinamik Veri Akış Hücresi
        Column(
            modifier = Modifier
                .weight(0.2f)
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .padding(start = 8.dp)
                // 🌟 PİKSEL KORUMA DENGELEMESİ: Sağ paneldeki yazılar zıt eksende hareket ederek taşmayı önler
                .offset(x = (-pixelOffsetX).dp, y = (-pixelOffsetY).dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            rightPanelContent()
        }
    }
}
// =========================================================================
// 🕰️ 2. PARÇA: DEVASA DİJİTAL SAAT BİLEŞENİ (DIGITAL CLOCK VIEW)
// =========================================================================
/**
 * Apple tarzı boylamasına ince-uzun ve milimetrik olarak alt-üst boşlukları
 * eşitlenmiş, piksel ömrü korumalı bağımsız dev dijital saat hücresi.
 */
@Composable
fun DigitalClockView(
    currentTime: String,
    clockColor: Color,
    pixelOffsetX: Float,
    pixelOffsetY: Float
) {
    Text(
        text = currentTime,
        fontSize = 330.sp, // Yatay ekranda maksimum dikey yüksekliğe ulaşan yazı boyutu
        fontWeight = FontWeight.Normal, // İnce ve modern Apple tasarım dili nizamı
        color = clockColor,
        letterSpacing = (-12).sp, // Rakamların ve iki noktanın birbirine tam nizamda yanaşması
        lineHeight = 330.sp, // Satır yüksekliğini sabitleyerek taşmaları önler
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.displayLarge.copy(
            // APPLE TARZI DİKİNE UZATMA: Harflerin yatay genişliğini %65'e daraltır.
            // Böylece rakamlar yana doğru taşmaz, dikeyde uzun ve estetik görünür.
            textGeometricTransform = androidx.compose.ui.text.style.TextGeometricTransform(
                scaleX = 0.65f, // 1.0f normaldir, 0.65f yatayda %35 daraltır
                skewX = 0f      // Eğim (0f düz tutar)
            ),
            // Fontun işletim sistemi tabanlı otomatik iç boşluklarını tamamen yok eder
            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                includeFontPadding = false
            ),
            // MİLİMETRİK EŞİTLEME: Üstteki fazladan boşluğu alttaki boşlukla matematiksel eşitler
            lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.CenterVertically) // Kutunun içindeki metni dikeyde ortalar
            .offset(
                x = pixelOffsetX.dp,
                y = pixelOffsetY.dp
            ) // Piksel ömrü koruma kayma koordinatları
    )
}

// ============================================================================
// 🌟 3. PARÇA: MİNİMALİST AYARLAR PENCERESİ (SETTINGSDIALOG)
// ============================================================================
/**
 * Kullanıcının otomatik GPS veya manuel şehir araması yapabilmesini sağlayan,
 * AMOLED tam siyah temayla uyumlu Material 3 diyalog penceresi bileşeni.
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
        containerColor = Color(0xFF121212), // Koyu diyalog arka yüzey rengi
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
                Text(
                    text = "Konum Algılama Yöntemi",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )

                // Tekli seçim Radio Button grubu
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // --- OPTION 1: OTOMATİK KONUM (GPS) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAutomaticLocation(true) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.isAutomatic,
                            onClick = { viewModel.toggleAutomaticLocation(true) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.White,
                                unselectedColor = Color.DarkGray
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Otomatik Konum (GPS)",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Canlı koordinat çözümlenir.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // --- OPTION 2: MANUEL ŞEHİR GİRİŞİ ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAutomaticLocation(false) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !config.isAutomatic,
                            onClick = { viewModel.toggleAutomaticLocation(false) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.White,
                                unselectedColor = Color.DarkGray
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Manuel Şehir Girişi",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Takvimi el ile yazdığınız şehre sabitler.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF2A2A2A))

                // Seçilen yönteme göre dinamik alt panel kontrolü
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
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("İptal", color = Color.Gray, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (config.isAutomatic) {
                            onDismiss()
                        } else {
                            viewModel.updateLocationManually(inputCity.value)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Kaydet",
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        },
        dismissButton = null
    )
}
