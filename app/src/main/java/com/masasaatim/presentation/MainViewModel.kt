package com.masasaatim.presentation

import android.app.Application
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masasaatim.MainApplication
import com.masasaatim.domain.model.PrayerTime
import com.masasaatim.domain.model.SavedLocation
import com.masasaatim.domain.usecase.GetPrayerTimeUseCase
import com.masasaatim.presentation.service.AzanPlaybackService
import com.masasaatim.domain.repository.PrayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainViewModel(
    application: Application,
    private val getPrayerTimeUseCase: GetPrayerTimeUseCase,
    private val prayerRepository: PrayerRepository
) : AndroidViewModel(application) {

    // --- GÜNCEL STANDART DEĞİŞKENLER ---
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _prayerTimes = MutableStateFlow<PrayerTime?>(null)
    val prayerTimes: StateFlow<PrayerTime?> = _prayerTimes.asStateFlow()

    private val _remainingTime = MutableStateFlow("Hesaplanıyor...")
    val remainingTime: StateFlow<String> = _remainingTime.asStateFlow()

    private val _isDimmedMode = MutableStateFlow(false)
    val isDimmedMode: StateFlow<Boolean> = _isDimmedMode.asStateFlow()

    private val _isAzanPlaying = MutableStateFlow(false)
    val isAzanPlaying: StateFlow<Boolean> = _isAzanPlaying.asStateFlow()

    private val _locationName = MutableStateFlow("Konum yükleniyor...")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    // 🌟 PİKSEL KORUMA: Saatin ve yazıların anlık kayma koordinatları (X ve Y ekseni)
    private val _pixelOffsetX = MutableStateFlow(0f)
    val pixelOffsetX: StateFlow<Float> = _pixelOffsetX.asStateFlow()

    private val _pixelOffsetY = MutableStateFlow(0f)
    val pixelOffsetY: StateFlow<Float> = _pixelOffsetY.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _nextVakitName = MutableStateFlow("")
    val nextVakitName: StateFlow<String> = _nextVakitName.asStateFlow()

    // --- BAŞLANGIÇ KONFİGÜRASYONU ---
    private val _currentConfig = MutableStateFlow(SavedLocation("Augustdorf", 51.9311, 8.8681, false))
    val currentConfig: StateFlow<SavedLocation> = _currentConfig.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateLiveTime()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        // Saniyelik canlı saat döngüsünü başlatır
        handler.post(timeRunnable)

        // GÜNCELLEME: Eğer otomatik konum (GPS) açıksa direkt arama moduna geçsin
        if (_currentConfig.value.isAutomatic) {
            _locationName.value = "GPS Aranıyor..."
        } else {
            // Eğer manuel moddaysa, hafızadaki mevcut şehrin adını direkt ekrana yansıtsın
            _locationName.value = _currentConfig.value.cityName
        }

        // Veritabanı veya internetten verileri çeken ana tetikleyici
        Handler(Looper.getMainLooper()).postDelayed({
            if (_prayerTimes.value == null) {
                android.util.Log.d("MasaSaatim", "Açılışta veritabanı boş. Konum tetikleniyor...")
                loadPrayerDataWithLocation(_currentConfig.value.latitude, _currentConfig.value.longitude)
            }
        }, 1500)
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _showSettingsDialog.value = visible
    }
    fun toggleAutomaticLocation(enable: Boolean) {
        _currentConfig.value = _currentConfig.value.copy(isAutomatic = enable)
        if (enable) {
            _locationName.value = "GPS Aranıyor..."
        }
    }

    /**
     * MANUEL SEÇENEK: Kullanıcı el ile şehir yazdığında koordinatları çözer.
     */
    fun updateLocationManually(cityName: String) {
        if (cityName.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val geocoder = Geocoder(context, Locale("tr"))
                val addresses = geocoder.getFromLocationName(cityName, 1)
                val address = addresses?.firstOrNull()

                if (address != null) {
                    val lat = address.latitude
                    val lon = address.longitude

                    val cleanCityName = cityName.trim().lowercase(Locale("tr")).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale("tr")) else it.toString()
                    }

                    prayerRepository.fetchAndSaveRemotePrayerTimes(lat, lon).onSuccess {
                        viewModelScope.launch(Dispatchers.Main) {
                            _currentConfig.value = SavedLocation(cleanCityName, lat, lon, false)
                            _locationName.value = cleanCityName
                            updateLiveTime()
                            setSettingsDialogVisible(false)
                        }
                    }.onFailure { error ->
                        android.util.Log.e("MasaSaatim", "Senkronizasyon hatası: ${error.localizedMessage}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MasaSaatim", "Geocoder hatası: ${e.localizedMessage}")
            }
        }
    }

    /**
     * OTOMATİK SEÇENEK: GPS donanımından gelen koordinatları işler.
     */
    fun updateLocationAutomatically(cityName: String, lat: Double, lon: Double) {
        if (_currentConfig.value.isAutomatic) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>().applicationContext
                    val geocoder = Geocoder(context, Locale("tr"))
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    val address = addresses?.firstOrNull()

                    val realCityName = address?.locality
                        ?: address?.subAdminArea
                        ?: address?.adminArea
                        ?: "Bilinmeyen Konum"

                    prayerRepository.fetchAndSaveRemotePrayerTimes(lat, lon).onSuccess {
                        viewModelScope.launch(Dispatchers.Main) {
                            _currentConfig.value = SavedLocation(realCityName, lat, lon, true)
                            _locationName.value = realCityName
                            updateLiveTime()
                        }
                    }.onFailure { error ->
                        android.util.Log.e("MasaSaatimGPS", "GPS veri indirme hatası: ${error.localizedMessage}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MasaSaatimGPS", "GPS Geocoder hatası: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun applyTemkinToTimeString(vakitName: String, timeStr: String): String {
        val diyanetTemkinPayi = mapOf(
            "imsak" to +39, "sunrise" to +1, "dhuhr" to 0, "asr" to 0, "maghrib" to 0, "isha" to -41
        )
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = sdf.parse(timeStr)
            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                val sapma = diyanetTemkinPayi[vakitName] ?: 0
                cal.add(Calendar.MINUTE, sapma)
                sdf.format(cal.time)
            } else timeStr
        } catch (e: Exception) {
            timeStr
        }
    }
    /**
     * VERİTABANI BAĞLANTISI: Room'dan verileri çeker veya eksikse internetten ister.
     */
    fun loadPrayerDataWithLocation(latitude: Double? = null, longitude: Double? = null) {
        if (latitude != null && longitude != null && _currentConfig.value.isAutomatic) {
            fetchCityNameFromCoordinates(latitude, longitude)
        }

        viewModelScope.launch(Dispatchers.Main) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            getPrayerTimeUseCase(todayStr).collect { prayerTime ->
                if (prayerTime != null) {
                    val correctedPrayerTime = PrayerTime(
                        date = prayerTime.date,
                        imsak = applyTemkinToTimeString("imsak", prayerTime.imsak),
                        gunes = applyTemkinToTimeString("sunrise", prayerTime.gunes),
                        ogle = applyTemkinToTimeString("dhuhr", prayerTime.ogle),
                        ikindi = applyTemkinToTimeString("asr", prayerTime.ikindi),
                        aksam = applyTemkinToTimeString("maghrib", prayerTime.aksam),
                        yatsi = applyTemkinToTimeString("isha", prayerTime.yatsi)
                    )
                    _prayerTimes.value = correctedPrayerTime
                } else {
                    val lat = latitude ?: 51.9311
                    val lon = longitude ?: 8.8681

                    viewModelScope.launch(Dispatchers.IO) {
                        prayerRepository.fetchAndSaveRemotePrayerTimes(lat, lon).onSuccess {
                            viewModelScope.launch(Dispatchers.Main) {
                                updateLiveTime()
                            }
                        }.onFailure { error ->
                            android.util.Log.e("MasaSaatim", "Geri plan yükleme hatası: ${error.localizedMessage}")
                        }
                    }
                }
            }
        }
    }

    private fun calculateRemainingTime(prayer: PrayerTime) {
        val now = Calendar.getInstance()
        val currentMs = now.timeInMillis
        val todayStr = SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(Date())

        val prayerList = listOf(
            Pair("imsak", prayer.imsak),
            Pair("sunrise", prayer.gunes),
            Pair("dhuhr", prayer.ogle),
            Pair("asr", prayer.ikindi),
            Pair("maghrib", prayer.aksam),
            Pair("isha", prayer.yatsi)
        )

        var nextVakitName = "imsak"
        var nextVakitMs: Long = 0

        for (vakit in prayerList) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val vakitDate = sdf.parse(todayStr + vakit.second)

                if (vakitDate != null && vakitDate.time > currentMs) {
                    nextVakitName = vakit.first
                    nextVakitMs = vakitDate.time
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val turkishVakitName = when (nextVakitName) {
            "imsak" -> "İmsak"
            "sunrise" -> "Güneş"
            "dhuhr" -> "Öğle"
            "asr" -> "İkindi"
            "maghrib" -> "Akşam"
            "isha" -> "Yatsı"
            else -> "İmsak"
        }
        _nextVakitName.value = turkishVakitName

        if (nextVakitMs == 0L) {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowStr = SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(tomorrow.time)
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val imsakDate = sdf.parse(tomorrowStr + prayer.imsak)
                if (imsakDate != null) {
                    nextVakitMs = imsakDate.time
                    nextVakitName = "imsak"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val diffMs = nextVakitMs - currentMs
        if (diffMs > 0) {
            if (diffMs <= 1000L && !_isAzanPlaying.value && nextVakitName != "sunrise") {
                triggerAzanService(nextVakitName)
            }

            val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMs) % 60
            _remainingTime.value = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            _remainingTime.value = "00:00:00"
        }
    }

    private fun updateLiveTime() {
        val now = Date()

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr"))
        _currentTime.value = timeFormat.format(now)
        _currentDate.value = dateFormat.format(now)

        // 🌟 PİKSEL KORUMA MOTORU: Her dakikanın ilk saniyesinde (Saniye 00) X ve Y eksenini rastgele kaydırır
        val calendar = Calendar.getInstance()
        val currentSecond = calendar.get(Calendar.SECOND)

        if (currentSecond == 0) {
            _pixelOffsetX.value = (-5..5).random().toFloat()
            _pixelOffsetY.value = (-5..5).random().toFloat()
            android.util.Log.d("MasaSaatimKoruma", "Piksel Kaydırma Aktif -> X: ${_pixelOffsetX.value}, Y: ${_pixelOffsetY.value}")
        }

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val shouldDim = currentHour !in 6..21
        if (_isDimmedMode.value != shouldDim) {
            _isDimmedMode.value = shouldDim
        }

        _prayerTimes.value?.let { calculateRemainingTime(it) }
    }

    private fun fetchCityNameFromCoordinates(latitude: Double, longitude: Double) {
        if (!_currentConfig.value.isAutomatic) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val geocoder = Geocoder(context, Locale("tr"))

                val extractCity: (android.location.Address?) -> String = { address ->
                    address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: "Bilinmeyen Konum"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        _locationName.value = extractCity(addresses.firstOrNull())
                    }
                } else {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    _locationName.value = extractCity(addresses?.firstOrNull())
                }
            } catch (e: Exception) {
                _locationName.value = "Konum Alınamadı"
            }
        }
    }

    private fun triggerAzanService(vakitName: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _isAzanPlaying.value = true
            val context = getApplication<Application>().applicationContext
            val intent = Intent(context, AzanPlaybackService::class.java).apply {
                action = AzanPlaybackService.ACTION_START_AZAN
                putExtra(AzanPlaybackService.EXTRA_PRAYER_TYPE, vakitName)
                putExtra(AzanPlaybackService.EXTRA_IS_DIMMED, _isDimmedMode.value)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun simulateAzanTrigger() {
        triggerAzanService("test_vakit")
    }

    fun stopAzanPlayback() {
        viewModelScope.launch(Dispatchers.Main) {
            _isAzanPlaying.value = false
            val context = getApplication<Application>().applicationContext
            val intent = Intent(context, AzanPlaybackService::class.java).apply {
                action = AzanPlaybackService.ACTION_STOP_AZAN
            }
            context.startService(intent)
        }
    }
    // 🌟 YENİ: Servis içinden ezan kendiliğinden bittiğinde arayüzü yeşile/eski rengine döndürür
    fun setAzanPlayingStatus(isPlaying: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            _isAzanPlaying.value = isPlaying
        }
    }


    override fun onCleared() {
        handler.removeCallbacks(timeRunnable)
        super.onCleared()
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContainer = (application as MainApplication).appContainer
                    return MainViewModel(
                        application,
                        appContainer.getPrayerTimeUseCase,
                        appContainer.prayerRepository
                    ) as T
                }
            }
        }
    }
}
