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

    // --- DETAYLI KLASİK EKRAN DEĞİŞKENLERİ ---
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

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    // --- MİNİMALİST GECE EKRANI DEĞİŞKENLERİ ---
    private val _isAlternativeUi = MutableStateFlow(false)
    val isAlternativeUi: StateFlow<Boolean> = _isAlternativeUi.asStateFlow()

    private val _minimalTime = MutableStateFlow("")
    val minimalTime: StateFlow<String> = _minimalTime.asStateFlow()

    private val _minimalDate = MutableStateFlow("")
    val minimalDate: StateFlow<String> = _minimalDate.asStateFlow()

    private val _nextVakitName = MutableStateFlow("")
    val nextVakitName: StateFlow<String> = _nextVakitName.asStateFlow()

    // --- VARSAYILAN BAŞLANGIÇ KONFİGÜRASYONU ---
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

        // Veritabanı boşsa arayüzün takılı kalmaması için İstanbul koordinatlarını yükler
        Handler(Looper.getMainLooper()).postDelayed({
            if (_prayerTimes.value == null) {
                android.util.Log.d("MasaSaatim", "Açılışta veritabanı boş. Tetikleniyor...")
                loadPrayerDataWithLocation(41.0082, 28.9784)
            }
        }, 1500)
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _showSettingsDialog.value = visible
    }

    fun toggleUiMode() {
        _isAlternativeUi.value = !_isAlternativeUi.value
    }

    fun toggleAutomaticLocation(enable: Boolean) {
        _currentConfig.value = _currentConfig.value.copy(isAutomatic = enable)
        if (enable) {
            _locationName.value = "GPS Aranıyor..."
        }
    }

    /**
     * MANUEL SEÇENEK: Kullanıcı el ile şehir yazdığında koordinatları çözer ve Repository'ye gönderir.
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
                    val finalCityName = address.locality ?: address.adminArea ?: cityName

                    // Eski kararlı koordinat yapısına güvenle geri dönüldü:
                    prayerRepository.fetchAndSaveRemotePrayerTimes(lat, lon).onSuccess {
                        viewModelScope.launch(Dispatchers.Main) {
                            _currentConfig.value = SavedLocation(finalCityName, lat, lon, false)
                            _locationName.value = finalCityName
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

                    // Eski kararlı koordinat yapısına güvenle geri dönüldü:
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
    /**
     * VERİTABANI BAĞLANTISI: Room'dan verileri çeker veya eksikse internetten koordinatla ister.
     */
    /**
     * DİYANET TEMKİN MOTORU (YARDIMCI FONKSİYON)
     * Bir saate (Örn: "04:12") ilgili vaktin Diyanet temkin dakikasını uygular ve yeni saati String döner.
     */
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
     * VERİTABANI BAĞLANTISI: Room'dan verileri çeker.
     * 🎯 ARTIK SAĞ PANELDEKİ YAZILAR DA BURADA TEMKİN MOTORUNDAN GEÇİRİLİP FİLTRELENİYOR!
     */
    fun loadPrayerDataWithLocation(latitude: Double? = null, longitude: Double? = null) {
        if (latitude != null && longitude != null) {
            fetchCityNameFromCoordinates(latitude, longitude)
        }

        viewModelScope.launch(Dispatchers.Main) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            getPrayerTimeUseCase(todayStr).collect { prayerTime ->
                if (prayerTime != null) {
                    // 🎯 TEMKİN ENJEKSİYONU: Sağ panelde listelenecek saatleri Diyanet paylarıyla düzeltiyoruz
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

    /**
     * DİYANET TEMKİN MOTORU DESTEKLİ GERİ SAYIM SİSTEMİ
     * Not: _prayerTimes artık yukarıda filtrelendiği için, bu fonksiyonun içindeki
     * listeyi doğrudan filtrelenmiş saatler üzerinden okutarak çelişkileri önlüyoruz.
     */
    private fun calculateRemainingTime(prayer: PrayerTime) {
        val now = Calendar.getInstance()
        val currentMs = now.timeInMillis
        val todayStr = SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(Date())

        // Saatler loadPrayerDataWithLocation içinde zaten düzeltildiği için ham temkin eklemelerini buradan kaldırıp
        // doğrudan arayüzdeki (filtrelenmiş) saatlerle senkron çalıştırıyoruz.
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
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr"))
        val now = Date()

        _currentTime.value = timeFormat.format(now)
        _currentDate.value = dateFormat.format(now)

        val minimalTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val minimalDateFormat = SimpleDateFormat("dd EEEE", Locale("tr"))
        _minimalTime.value = minimalTimeFormat.format(now)
        _minimalDate.value = minimalDateFormat.format(now)

        // Gece 22:00 ile sabah 05:59 arası otomatik karartma
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val shouldDim = currentHour !in 6..21
        if (_isDimmedMode.value != shouldDim) {
            _isDimmedMode.value = shouldDim
        }

        _prayerTimes.value?.let { calculateRemainingTime(it) }
    }
    /**
     * DİYANET TEMKİN MOTORU DESTEKLİ GERİ SAYIM SİSTEMİ
     * API'den gelen ham vakitleri fıkhi temkin paylarıyla düzelterek işler.
     */



    private fun fetchCityNameFromCoordinates(latitude: Double, longitude: Double) {
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

