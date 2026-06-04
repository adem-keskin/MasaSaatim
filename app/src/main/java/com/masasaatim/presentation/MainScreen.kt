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

@Composable
fun MainScreen() {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(context))

    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Saf AMOLED Siyahı ile pil tasarrufu
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SOL PANEL: Canlı Saat ve Tarih Gösterimi
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime,
                fontSize = 84.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentDate,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary, // Dikkat çeken şık neon yeşil detay
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = remainingTime,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        // SAĞ PANEL: Ezan Vakitleri Listesi
        Column(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxHeight()
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            prayerTimes?.let { times ->
                PrayerTimeRow(name = "İmsak", time = times.imsak)
                PrayerTimeRow(name = "Güneş", time = times.gunes)
                PrayerTimeRow(name = "Öğle", time = times.ogle)
                PrayerTimeRow(name = "İkindi", time = times.ikindi)
                PrayerTimeRow(name = "Akşam", time = times.aksam)
                PrayerTimeRow(name = "Yatsı", time = times.yatsi)
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
fun PrayerTimeRow(name: String, time: String) {
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = time,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        HorizontalDivider(color = Color(0xFF1E1E1E), thickness = 0.5.dp)
    }
}
