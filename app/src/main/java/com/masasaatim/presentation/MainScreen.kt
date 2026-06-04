package com.masasaatim.presentation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
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

@Composable
fun MainScreen() {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(context))

    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val isDimmedMode by viewModel.isDimmedMode.collectAsState()
    val isAzanPlaying by viewModel.isAzanPlaying.collectAsState()

    // AUTO-DIMMING RENK MOTORU
    val clockColor = if (isDimmedMode) Color(0xFF444444) else Color(0xFFFFFFFF)
    val primaryDetailColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFF00E676)
    val listLabelColor = if (isDimmedMode) Color(0xFF333333) else Color(0xB3FFFFFF)
    val dividerColor = if (isDimmedMode) Color(0xFF0A0A0A) else Color(0xFF1E1E1E)

    // İkon Buton Renkleri (Gece gözü almaması için çok loş yapıldı)
    val iconActiveColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFF00E676)
    val iconPassiveColor = if (isDimmedMode) Color(0xFF222222) else Color(0xFF555555)

    // AMOLED EKRAN KORUYUCU
    val currentMinute = Calendar.getInstance().get(Calendar.MINUTE)
    val offsetX = when (currentMinute % 4) {
        0 -> 6.dp; 1 -> (-6).dp; 2 -> 4.dp; else -> (-4).dp
    }
    val offsetY = when (currentMinute % 3) {
        0 -> 4.dp; 1 -> (-4).dp; else -> 2.dp
    }

    // Tüm ekranı kapsayan Box (Sağ alt köşeye buton sabitlemek için en dış katman yapıldı)
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
        ) {
            // SOL PANEL: Canlı Saat ve Tarih
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
                    color = clockColor,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentDate,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryDetailColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = remainingTime,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDimmedMode) Color(0xFF222222) else Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // SAĞ PANEL: Ezan Vakitleri Listesi
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 24.dp) // Sağ köşe ikonlarına alan açmak için end padding eklendi
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
                }
            }
        }

        // --- SOL ALT KÖŞE MİNİMALİST KONTROL PANELİ ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)   //  Sağdan sola alındı!
                .padding(bottom = 16.dp, start = 16.dp)  // end padding start yapıldı
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play (Test Tetikleyici) İkon Butonu
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

            // Stop (Susturucu) İkon Butonu
            IconButton(
                onClick = { viewModel.stopAzanPlayback() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, // X (Kapat/Durdur İkonu)
                    contentDescription = "Durdur",
                    tint = if (isAzanPlaying) Color.Red else iconPassiveColor,
                    modifier = Modifier.size(24.dp)
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
                color = labelColor
            )
            Text(
                text = time,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
        HorizontalDivider(color = divColor, thickness = 0.5.dp)
    }
}
