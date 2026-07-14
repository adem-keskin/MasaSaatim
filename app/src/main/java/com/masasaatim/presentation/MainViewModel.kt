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
import com.masasaatim.domain.repository.PrayerRepository
import com.masasaatim.domain.usecase.GetPrayerTimeUseCase
import com.masasaatim.presentation.service.AzanPlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Masa Saati uygulamasının ana ekran verilerini ve iş mantığını yöneten merkezi ViewModel sınıfı.
 */
class MainViewModel(
    application: Application,
    private val getPrayerTimeUseCase: GetPrayerTimeUseCase,
    private val prayerRepository: PrayerRepository
) : AndroidViewModel(application) {

    // --- CANLI SAAT VE TARİH AKIŞLARI ---
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    // --- EZAN VAKTİ VE GERİ SAYIM AKIŞLARI ---
    private val _prayerTimes = MutableStateFlow<PrayerTime?>(null)
    val prayerTimes: StateFlow<PrayerTime?> = _prayerTimes.asStateFlow()

    private val _remainingTime = MutableStateFlow("00:00:00")
    val remainingTime: StateFlow<String> = _remainingTime.asStateFlow()

    private val _nextVakitName = MutableStateFlow("")
    val nextVakitName: StateFlow<String> = _nextVakitName.asStateFlow()

    // --- KULLANICI ARAYÜZÜ VE MOD DURUMLARI ---
    private val _isDimmedMode = MutableStateFlow(false)
    val isDimmedMode: StateFlow<Boolean> = _isDimmedMode.asStateFlow()

    private val _isAzanPlaying = MutableStateFlow(false)
    val isAzanPlaying: StateFlow<Boolean> = _isAzanPlaying.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    // --- KONUM VE GPS AYARLARI ---
    private val _locationName = MutableStateFlow("")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _currentConfig = MutableStateFlow(SavedLocation("Augustdorf", 51.9311, 8.8681, false))
    val currentConfig: StateFlow<SavedLocation> = _currentConfig.asStateFlow()

    // --- PİKSEL KORUMA (AMOLED REFRESH SHIFT) KOORDİNATLARI ---
    private val _pixelOffsetX = MutableStateFlow(0f)
    val pixelOffsetX: StateFlow<Float> = _pixelOffsetX.asStateFlow()

    private val _pixelOffsetY = MutableStateFlow(0f)
    val pixelOffsetY: StateFlow<Float> = _pixelOffsetY.asStateFlow()

    // --- ZAMANLAYICI VE DÖNGÜ MEKANİZMASI ---
    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateLiveTime() // Her saniye saat, tarih ve sayaçları canlı tetikler
            handler.postDelayed(this, 1000)
        }
    }

    // =========================================================================
    // INITIALIZER (BAŞLATICI BLOK)
    // =========================================================================
    init {
        handler.post(timeRunnable)

        if (_currentConfig.value.isAutomatic) {
            _locationName.value = "GPS"
        } else {
            _locationName.value = _currentConfig.value.cityName
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (_prayerTimes.value == null) {
                loadPrayerDataWithLocation(
                    _currentConfig.value.latitude,
                    _currentConfig.value.longitude
                )
            }
        }, 1500)
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _showSettingsDialog.value = visible
    }

    fun toggleAutomaticLocation(enable: Boolean) {
        _currentConfig.value = _currentConfig.value.copy(isAutomatic = enable)
        if (enable) {
            _locationName.value = "GPS"
        }
    }

    fun setAzanPlayingStatus(isPlaying: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            _isAzanPlaying.value = isPlaying
        }
    }

    // =========================================================================
    // COĞRAFİ KONUM VE MANUEL SEÇİM MOTORLARI
    // =========================================================================

    fun updateLocationManually(cityName: String) {
        if (cityName.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val geocoder = Geocoder(context, Locale("tr"))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(cityName, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        if (address != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                processManualLocationFetch(cityName, address.latitude, address.longitude)
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(cityName, 1)
                    val address = addresses?.firstOrNull()
                    if (address != null) {
                        processManualLocationFetch(cityName, address.latitude, address.longitude)
                    }
                }
            } catch (_: Exception) {
                // Güvenli pas geçme
            }
        }
    }

    private fun processManualLocationFetch(cityName: String, lat: Double, lon: Double) {
        val cleanCityName = cityName.trim().lowercase(Locale("tr")).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale("tr")) else it.toString()
        }

        viewModelScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) + 1

            prayerRepository.fetchAndSaveRemotePrayerTimes(
                latitude = lat,
                longitude = lon,
                month = currentMonth,
                year = currentYear
            ).onSuccess {
                viewModelScope.launch(Dispatchers.Main) {
                    _currentConfig.value = SavedLocation(cleanCityName, lat, lon, false)
                    _locationName.value = cleanCityName
                    updateLiveTime()
                    setSettingsDialogVisible(false)
                }
            }
        }
    }    // =========================================================================
    // OTOMATİK KONUM (GPS) VE CANLI ZAMANLAYICI METOTLARI
    // =========================================================================

    /**
     * Donanımsal GPS servisinden (MainActivity) gelen enlem ve boylama göre konumu otomatik günceller.
     */
    fun updateLocationAutomatically(cityName: String, lat: Double, lon: Double) {
        if (_currentConfig.value.isAutomatic) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val realCityName = fetchCityNameSuspended(lat, lon) ?: cityName
                    executeRemoteDataSync(realCityName, lat, lon)
                } catch (_: Exception) {
                    // Güvenli pas geçme
                }
            }
        }
    }

    /**
     * GPS'ten gelen kararlı koordinat verilerini dinamik ay/yıl bilgisiyle ağ katmanına senkronize eder.
     */
    private suspend fun executeRemoteDataSync(realName: String, lat: Double, lon: Double) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        prayerRepository.fetchAndSaveRemotePrayerTimes(
            latitude = lat,
            longitude = lon,
            month = currentMonth,
            year = currentYear
        ).onSuccess {
            viewModelScope.launch(Dispatchers.Main) {
                _currentConfig.value = SavedLocation(realName, lat, lon, true)
                _locationName.value = realName
                updateLiveTime()
            }
        }
    }

    /**
     * Verilen enlem ve boylam bilgisini Coroutine akışını kesmeden arka planda şehir ismine dönüştürür.
     */
    private suspend fun fetchCityNameSuspended(lat: Double, lon: Double): String? =
        suspendCancellableCoroutine { continuation ->
            try {
                val context = getApplication<Application>().applicationContext
                val geocoder = Geocoder(context, Locale("tr"))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val name = address?.locality ?: address?.subAdminArea ?: address?.adminArea
                        if (continuation.isActive) continuation.resume(name)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    val address = addresses?.firstOrNull()
                    val name = address?.locality ?: address?.subAdminArea ?: address?.adminArea
                    if (continuation.isActive) continuation.resume(name)
                }
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    /**
     * Verilen konuma göre yerel veritabanını (Room) sorgular.
     * MainActivity (Satır 191) erişimi için public yapılmıştır.
     */
    fun loadPrayerDataWithLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = isoDateFormat.format(Date())

            getPrayerTimeUseCase(todayStr).collect { prayerTime ->
                if (prayerTime != null) {
                    _prayerTimes.value = prayerTime
                    calculateRemainingTimeAndNextVakit(prayerTime)
                } else {
                    val calendar = Calendar.getInstance()
                    val currentYear = calendar.get(Calendar.YEAR)
                    val currentMonth = calendar.get(Calendar.MONTH) + 1

                    prayerRepository.fetchAndSaveRemotePrayerTimes(
                        latitude = lat,
                        longitude = lon,
                        month = currentMonth,
                        year = currentYear
                    )
                }
            }
        }
    }

    /**
     * Her saniye tetiklenerek arayüzün canlı saat, tarih ve piksel koruma koordinatlarını yeniler.
     */
    /**
     * Her saniye tetiklenerek arayüzün canlı saat, tarih ve piksel koruma koordinatlarını yeniler.
     * 🌟 GECE MODU: Gece 22:00 ile Sabah 06:00 arasında ışığı ve ses seviyesini otomatik loşlaştırır.
     */
    fun updateLiveTime() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE,dd MMMM", Locale("tr", "TR"))
        val now = Date()

        _currentTime.value = timeFormat.format(now)
        _currentDate.value = dateFormat.format(now)

        // 🌟 GECE MODU OTLOMATİK DENETİMİ:
        val currentCalendar = Calendar.getInstance()
        val hour = currentCalendar.get(Calendar.HOUR_OF_DAY)

        // Eğer saat 22 veya daha büyükse YA DA saat sabah 6'dan küçükse loş modu otomatik açar
        val shouldBeDimmed = hour >= 22 || hour < 6
        if (_isDimmedMode.value != shouldBeDimmed) {
            _isDimmedMode.value = shouldBeDimmed
        }

        // PİKSEL YANMASINI ÖNLEME (Burn-in Protection):
        val second = currentCalendar.get(Calendar.SECOND)
        if (second == 0) {
            _pixelOffsetX.value = (-3..3).random().toFloat()
            _pixelOffsetY.value = (-3..3).random().toFloat()
        }

        _prayerTimes.value?.let { calculateRemainingTimeAndNextVakit(it) }
    }

    private fun calculateRemainingTimeAndNextVakit(prayerTime: PrayerTime) {
        val now = Calendar.getInstance()
        val currentMs = now.timeInMillis

        // Önbellekteki ISO tarih formatını alıyoruz (yyyy-MM-dd )
        val isoDateFormat = SimpleDateFormat("yyyy-MM-dd ", Locale.US)
        val todayStr = isoDateFormat.format(Date())

        // 6 Vakitlik kronolojik tarama listesi (İsim ve Saat eşleşmesi)
        // ✅ ÖNERİLEN AYAR: Widget ile birebir eşitlemek için:
        val vakitList = listOf(
            Pair("İmsak", applyTemkin(39, prayerTime.imsak)),   // İmsaka 39 dakika ekler
            Pair("Güneş", applyTemkin(1, prayerTime.gunes)),   // Güneşe 1 dakika ekler
            Pair("Öğle", applyTemkin(0, prayerTime.ogle)),
            Pair("İkindi", applyTemkin(0, prayerTime.ikindi)),
            Pair("Akşam", applyTemkin(1, prayerTime.aksam)),
            Pair("Yatsı", applyTemkin(-41, prayerTime.yatsi))  // Yatsıdan 41 dakika çıkarır
        )

        var detectedNextVakit = "İmsak"
        var nextVakitTargetMs: Long = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        // Bugün içindeki vakitleri sırayla tarıyoruz
        for (vakit in vakitList) {
            try {
                val vakitDate = sdf.parse(todayStr + vakit.second)
                // Eğer vaktin saati şu anki sistem saatinden büyükse, sıradaki vakit odur!
                if (vakitDate != null && vakitDate.time > currentMs) {
                    detectedNextVakit = vakit.first
                    nextVakitTargetMs = vakitDate.time
                    break
                }
            } catch (_: Exception) { /* Güvenli pas geçme */ }
        }

        // GECE YARISI KONTROLÜ: Eğer bugün tüm vakitler bittiyse (Yatsıdan sonra ve gece 00:00'dan sonra)
        // Hedef artık kesinlikle yarının İmsak vaktidir!
        if (nextVakitTargetMs == 0L) {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowStr = isoDateFormat.format(tomorrow.time)
            try {
                val imsakDate = sdf.parse(tomorrowStr + prayerTime.imsak)
                if (imsakDate != null) {
                    nextVakitTargetMs = imsakDate.time
                    detectedNextVakit = "İmsak"
                }
            } catch (_: Exception) { /* Güvenli pas geçme */ }
        }

        // Milisaniye cinsinden kalan zaman farkını hesaplıyoruz
        val diffMs = nextVakitTargetMs - currentMs

        if (diffMs > 1000) { // 1 saniyeden fazla zaman varsa geri sayımı saniyelik düşürür
            val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMs) % 60

            // Canlı akışları (StateFlow) saniyelik güncelliyoruz
            _nextVakitName.value = detectedNextVakit
            _remainingTime.value = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else if (diffMs in 0..1000) {
            // OTOMATİK EZAN TETİKLEME: Sayaç tam sıfıra ulaştığı an gerçek ezanı otomatik başlatır
            _remainingTime.value = "00:00:00"
            if (!_isAzanPlaying.value) {
                triggerAzanService(detectedNextVakit)
            }
        } else {
            _remainingTime.value = "00:00:00"
        }
    }

    /**
     * GPS koordinatlarından şehir ismini asenkron olarak çözer ve ekrandaki State etiketine yansıtır.
     */
    private fun fetchCityNameFromCoordinates(latitude: Double, longitude: Double) {
        if (!_currentConfig.value.isAutomatic) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val geocoder = Geocoder(context, Locale("tr"))

                val extractCity: (android.location.Address?) -> String = { address ->
                    address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: ""
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        viewModelScope.launch(Dispatchers.Main) {
                            _locationName.value = extractCity(addresses.firstOrNull())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    viewModelScope.launch(Dispatchers.Main) {
                        _locationName.value = extractCity(addresses?.firstOrNull())
                    }
                }
            } catch (_: Exception) {
                // Güvenli pas geçme
            }
        }
    }

    /**
     * Ezan vakti geldiğinde veya test modunda multimedya oynatıcı (ExoPlayer) ön plan servisini başlatır.
     */
    private fun triggerAzanService(vakitName: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _isAzanPlaying.value = true
            val context = getApplication<Application>().applicationContext
            val intent = Intent(context, AzanPlaybackService::class.java).apply {
                action = AzanPlaybackService.ACTION_START_AZAN
                putExtra(AzanPlaybackService.EXTRA_PRAYER_TYPE, vakitName)
                putExtra(AzanPlaybackService.EXTRA_IS_DIMMED, _isDimmedMode.value)
            }
            context.startForegroundService(intent)
        }
    }

    /**
     * Kullanıcı sağ paneldeki oynat butonuna bastığında test vakti adıyla servisi manuel ateşler.
     */
    fun simulateAzanTrigger() {
        triggerAzanService("test_vakit")
    }

    /**
     * Kullanıcı sustur butonuna bastığında veya ezan bittiğinde oynatıcı servisini tamamen kapatır.
     */
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

    /**
     * Bellek sızıntılarını önlemek için canlı saat döngüsü Handler referansını hafızadan temizler.
     */
    override fun onCleared() {
        handler.removeCallbacks(timeRunnable)
        super.onCleared()
    }
    /**
     * 🌟 YENİ: Ezan vakitlerine el ile dakika ekleyen veya çıkaran yardımcı temkin fonksiyonu.
     */
    private fun applyTemkin(minutes: Int, timeStr: String): String {
        if (minutes == 0) return timeStr
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = sdf.parse(timeStr) ?: return timeStr
            val cal = Calendar.getInstance().apply { time = date }
            cal.add(Calendar.MINUTE, minutes)
            sdf.format(cal.time)
        } catch (_: Exception) {
            timeStr
        }
    }

    // =========================================================================
    // VIEWMODEL FABRİKA (FACTORY) YAPILANDIRMASI
    // =========================================================================
    companion object {
        @JvmStatic
        @Suppress("unused")
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