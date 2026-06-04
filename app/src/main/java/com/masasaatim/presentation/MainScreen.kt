package com.masasaatim.presentation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.masasaatim.domain.model.PrayerTime
import java.util.Calendar

@Composable
fun MainScreen() {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(context))

    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()

    // Gece modu durumunu Viewmodel üzerinden reaktif olarak dinliyoruz
    val isDimmedMode by viewModel.isDimmedMode.collectAsState()

    // --- SENIOR AUTO-DIMMING RENK MOTORU ---
    // Eğer gece modundaysak, piksellerin ışık şiddetini %80 oranında düşürüyoruz.
    val clockColor = if (isDimmedMode) Color(0xFF444444) else Color(0xFFFFFFFF)       // Beyaz -> Mat Koyu Gri
    val primaryDetailColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFF00E676) // Neon Yeşil -> Loş Orman Yeşili
    val listLabelColor = if (isDimmedMode) Color(0xFF333333) else Color(0xB3FFFFFF)     // Flu Beyaz -> Çok Koyu Gri
    val dividerColor = if (isDimmedMode) Color(0xFF0A0A0A) else Color(0xFF1E1E1E)       // İnce Çizgi -> Neredeyse Siyah
    // ----------------------------------------

    // PREMIUM AMOLED BURN-IN SCHUTZ-ALGORITHMUS (Mevcut koruma)
    val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
    val offsetX = when (currentMinute % 4) {
        0 -> 6.dp
        1 -> (-6).dp
        2 -> 4.dp
        else -> (-4).dp
    }
    val offsetY = when (currentMinute % 3) {
        0 -> 4.dp
        1 -> (-4).dp
        else -> 2.dp
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Saf AMOLED Siyahı arka planı korur
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SOL PANEL: Canlı Saat ve Tarih Gösterimi (Dinamik Renkli)
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .offset(x = offsetX, y = offsetY),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime,
                fontSize = 84.sp,
                fontWeight = FontWeight.Light,
                color = clockColor, // Dinamik Renk
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentDate,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = primaryDetailColor, // Dinamik Renk
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = remainingTime,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDimmedMode) Color(0xFF222222) else Color.Gray, // Dinamik Sayaç Rengi
                textAlign = TextAlign.Center
            )
        }

        // SAĞ PANEL: Ezan Vakitleri Listesi (Dinamik Renkli)
        Column(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxHeight()
                .padding(start = 16.dp)
                .offset(x = -offsetX, y = -offsetY),
            verticalArrangement = Arrangement.Center
        ) {
            prayerTimes?.let { times ->
                PrayerTimeRow(name = "İmsak", time = times.imsak, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                PrayerTimeRow(name = "Güneş", time = times.gunes, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                PrayerTimeRow(name = "Öğle", time = times.ogle, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                PrayerTimeRow(name = "İkindi", time = times.ikindi, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                PrayerTimeRow(name = "Akşam", time = times.aksam, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
                PrayerTimeRow(name = "Yatsı", time = times.yatsi, labelColor = listLabelColor, valueColor = clockColor, divColor = dividerColor)
            } ?: run {
                Text(
                    text = "Vakitler yükleniyor...",
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

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
                color = labelColor // Gece moduna duyarlı etiket rengi
            )
            Text(
                text = time,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor // Gece moduna duyarlı vakit saati rengi
            )
        }
        HorizontalDivider(color = divColor, thickness = 0.5.dp) // Gece moduna duyarlı ince çizgi
    }
}
