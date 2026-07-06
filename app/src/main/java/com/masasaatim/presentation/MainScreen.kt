package com.masasaatim.presentation

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
import java.text.SimpleDateFormat

@Composable
fun MainScreen() {

    val localContext = LocalContext.current
    val window = (localContext as? Activity)?.window
    if (window != null) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    val mainViewModel: MainViewModel = viewModel()

    val showSettingsDialog by mainViewModel.showSettingsDialog.collectAsState()
    val remainingTime by mainViewModel.remainingTime.collectAsState()
    val isDimmedMode by mainViewModel.isDimmedMode.collectAsState()
    val isAzanPlaying by mainViewModel.isAzanPlaying.collectAsState()
    val locationName by mainViewModel.locationName.collectAsState()
    val currentTime by mainViewModel.currentTime.collectAsState()
    val currentDate by mainViewModel.currentDate.collectAsState()
    val nextVakitName by mainViewModel.nextVakitName.collectAsState()

    // 🌟 PİKSEL KORUMA: Anlık kayma koordinatları arayüze bağlanıyor
    val pixelOffsetX by mainViewModel.pixelOffsetX.collectAsState()
    val pixelOffsetY by mainViewModel.pixelOffsetY.collectAsState()

    val clockColor = if (isDimmedMode) Color(0xFF444444) else Color(0xFFFFFFFF)
    val detailColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFFCDDC39)

    val iconActiveColor = if (isDimmedMode) Color(0xFF005511) else Color(0xFFCDDC39)
    val iconPassiveColor = if (isDimmedMode) Color(0xFF222222) else Color(0xFF555555)

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = mainViewModel,
            onDismiss = { mainViewModel.setSettingsDialogVisible(false) }
        )
    }
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

            // SOL PANEL (%80 Genişlik): Devasa Dijital Saat
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTime,
                    fontSize = 200.sp,
                    fontWeight = FontWeight.Bold,
                    color = clockColor,
                    letterSpacing = (-2).sp,
                    lineHeight = 170.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.CenterVertically)
                        // 🌟 SAAT HER DAKİKA MİNİMAL ÖLÇÜDE KAYARAK PİKSEL ÖMRÜNÜ KORUR:
                        .offset(x = pixelOffsetX.dp, y = pixelOffsetY.dp)
                )
            }

            // SAĞ PANEL (%20 Genişlik): Hatasız Dikey 3'lü Bölüm Nizamı
            Column(
                modifier = Modifier
                    .weight(0.2f)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .padding(start = 8.dp)
                    // 🌟 SAĞ PANELDEKİ YAZILAR DA KORUMA ALTINA ALINDI (Zıt yönlü kaydırma dengesi):
                    .offset(x = (-pixelOffsetX).dp, y = (-pixelOffsetY).dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {

                // 1. BÖLÜM (ÜST): Sağa Yaslı Detaylı Tarih
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val numericDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val formattedNumericDate = numericDateFormat.format(java.util.Date())

                    val dateParts = currentDate.split(",")
                    val rawDayName = dateParts.getOrNull(1)?.trim() ?: ""

                    val formattedDayName = rawDayName.lowercase(Locale("tr")).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale("tr")) else it.toString()
                    }

                    Text(
                        text = formattedNumericDate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = clockColor,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = formattedDayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = detailColor,
                        textAlign = TextAlign.End
                    )
                }

                // 2. BÖLÜM (ORTA): Konum İkonu ve Tek Şehir İsmi
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { mainViewModel.setSettingsDialogVisible(true) }
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ayarlar",
                        tint = detailColor,
                        modifier = Modifier.size(18.dp)
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
                // 3. BÖLÜM (ALT): İkonlar, Vakit Adı ve Sayaç
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
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
        }
    }
}

// ============================================================================
// 🌟 MINIMALIST AYARLAR PENCERESİ (SettingsDialog)
// ============================================================================
@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val config by viewModel.currentConfig.collectAsState()
    val inputCity = remember { mutableStateOf(config.cityName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
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

                Column(
                    modifier = Modifier.selectableGroup().fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                    Text("Kaydet", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        },
        dismissButton = null
    )
}
