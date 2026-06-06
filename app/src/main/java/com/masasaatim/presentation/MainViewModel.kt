package com.masasaatim.presentation

// Android sistem bileşenleri, Yaşam döngüsü (Lifecycle), Coroutine ve Zaman hesaplama kütüphaneleri içe aktarılıyor.
import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masasaatim.MainApplication
import com.masasaatim.domain.model.PrayerTime
import com.masasaatim.domain.usecase.FetchPrayerTimesUseCase
import com.masasaatim.domain.usecase.GetPrayerTimeUseCase
import com.masasaatim.presentation.service.AzanPlaybackService
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

/**
 * Final Senior-Level ViewModel: Saat durumunu, otomatik ekran karartmayı (Auto-Dimming),
 * AMOLED ekran koruyucuyu, arka plan ezan ses servisini ve donmayan asenkron konum/ağ senkronizasyonunu yönetir.
 */
class MainViewModel(
    application: Application,
    private val getPrayerTimeUseCase: GetPrayerTimeUseCase,
    private val fetchPrayerTimesUseCase: FetchPrayerTimesUseCase
) : AndroidViewModel(application) {

    // --- REAKTİF DURUM AKIŞLARI (StateFlow) ---
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _prayerTimes = MutableStateFlow<PrayerTime?>(null)
    val prayerTimes: StateFlow<PrayerTime?> = _prayerTimes.asStateFlow()

    private val _remainingTime = MutableStateFlow("Calculating...")
    val remainingTime: StateFlow<String> = _remainingTime.asStateFlow()

    private val _isDimmedMode = MutableStateFlow(false)
    val isDimmedMode: StateFlow<Boolean> = _isDimmedMode.asStateFlow()

    private val _isAzanPlaying = MutableStateFlow(false)
    val isAzanPlaying: StateFlow<Boolean> = _isAzanPlaying.asStateFlow()

    // --- ZAMAN DÖNGÜSÜ (Saniyelik Saat Güncelleyici) ---
    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateLiveTime()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        // Döngüyü başlat
        handler.post(timeRunnable)

        // --- ASENKRON GÜVENLİK AĞI (Safety Net) ---
        // Uygulama açıldıktan 1.5 saniye sonra veri tabanı hala boşsa otomatik senkronizasyonu tetikler.
        Handler(Looper.getMainLooper()).postDelayed({
            if (_prayerTimes.value == null) {
                android.util.Log.d("MasaSaatim", "Açılışta veri tabanı boş çıktı. Otomatik senkronizasyon zorlanıyor...")
                loadPrayerDataWithLocation(41.0082, 28.9784) // Yedek İstanbul koordinatları
            }
        }, 1500)
    }

    /**
     * updateLiveTime: Canlı saati ve otomatik loş modu (Auto-Dimming) kontrol eder.
     */
    private fun updateLiveTime() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr"))
        val now = Date()
        _currentTime.value = timeFormat.format(now)
        _currentDate.value = dateFormat.format(now)

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // Akşam 22:00 ile Sabah 06:00 arası loş modu aktif et
        val shouldDim = currentHour !in 6..21
        if (_isDimmedMode.value != shouldDim) {
            _isDimmedMode.value = shouldDim
        }

        _prayerTimes.value?.let { calculateRemainingTime(it) }
    }

    /**
     * loadPrayerDataWithLocation: Room veri tabanını canlı dinler, veri yoksa API'ye istek atar.
     */
    fun loadPrayerDataWithLocation(latitude: Double? = null, longitude: Double? = null) {
        viewModelScope.launch(Dispatchers.Main) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            getPrayerTimeUseCase(todayStr).collect { prayerTime ->
                if (prayerTime != null) {
                    _prayerTimes.value = prayerTime
                } else {
                    val lat = latitude ?: 51.9311
                    val lon = longitude ?: 8.8681

                    viewModelScope.launch(Dispatchers.IO) {
                        fetchPrayerTimesUseCase(lat, lon).onSuccess {
                            viewModelScope.launch(Dispatchers.Main) {
                                updateLiveTime()
                            }
                        }.onFailure { error ->
                            android.util.Log.e("MasaSaatim", "Senkronizasyon Hatası: ${error.localizedMessage}")
                        }
                    }
                }
            }
        }
    }

    /**
     * calculateRemainingTime: Geri sayımı hesaplar ve tam ezan saniyesinde servisi tetikler.
     */
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
                val vakitDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .parse(todayStr + vakit.second)
                if (vakitDate != null && vakitDate.time > currentMs) {
                    nextVakitName = vakit.first
                    nextVakitMs = vakitDate.time
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (nextVakitMs == 0L) {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowStr = SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(tomorrow.time)
            val imsakDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .parse(tomorrowStr + prayer.imsak)
            nextVakitMs = imsakDate?.time ?: 0L
            nextVakitName = "imsak"
        }

        val diff = nextVakitMs - currentMs

        // --- MILISANIYELIK TAM ZAMANLI TETIKLEYICI ---
        if (diff in 0..999 && currentMs % 60000 < 1500) {
            _isAzanPlaying.value = true
            triggerAzanService(nextVakitName)
        }

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

        _remainingTime.value = String.format(
            Locale.getDefault(),
            "%s vaktine %02d:%02d:%02d kaldı",
            nextVakitName.uppercase(Locale.getDefault()),
            hours,
            minutes,
            seconds
        )
    }

    /**
     * triggerAzanService: Foreground (Ön Plan) ezan ses servisini başlatır.
     */
    private fun triggerAzanService(prayerType: String) {
        val intent = Intent(getApplication(), AzanPlaybackService::class.java).apply {
            putExtra(AzanPlaybackService.EXTRA_PRAYER_TYPE, prayerType.lowercase(Locale.getDefault()))
            putExtra(AzanPlaybackService.EXTRA_IS_DIMMED, _isDimmedMode.value)
        }
        getApplication<Application>().startForegroundService(intent)
    }

    /**
     * simulateAzanTrigger: Ekrandaki 'Oynat' ikonuna basıldığında ezanı anında test etmek için tetiklenir.
     */
    fun simulateAzanTrigger() {
        viewModelScope.launch {
            val currentVakit = _remainingTime.value.substringBefore(" ").lowercase(Locale.getDefault())
            val targetVakit = if (currentVakit.contains("calculating") || currentVakit.contains("vaktine")) "dhuhr" else currentVakit

            _isAzanPlaying.value = true
            triggerAzanService(targetVakit)
        }
    }

    /**
     * stopAzanPlayback: Ekrandaki 'Durdur' butonuna basıldığında çalan ezan servisini anında susturur.
     */
    fun stopAzanPlayback() {
        _isAzanPlaying.value = false
        val intent = Intent(getApplication(), AzanPlaybackService::class.java).apply {
            action = AzanPlaybackService.ACTION_STOP_AZAN
        }
        getApplication<Application>().startService(intent)
    }

    /**
     * onCleared: Bellek sızıntılarını önlemek için zamanlayıcı döngüyü temizler.
     */
    override fun onCleared() {
        handler.removeCallbacks(timeRunnable)
        super.onCleared()
    }
    // --- YENİ KONUM STATEFLOW AKIŞINI EKLEYİN (Diğer StateFlow'ların altına koyabilirsiniz) ---
    private val _locationName = MutableStateFlow("Hesaplanıyor...")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    // --- GÜNCELLENMİŞ COĞRAFİ KODLAYICILI KONUM MOTORU ---
    fun loadPrayerDataWithLocation(latitude: Double? = null, longitude: Double? = null) {
        viewModelScope.launch(Dispatchers.Main) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val lat = latitude ?: 51.9311
            val lon = longitude ?: 8.8681

            // --- SENIOR GEOCODER MOTORU: Koordinatı asenkron olarak şehir ismine çevirir ---
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Cihazın sistem diline göre (Locale.getDefault()) bir coğrafi kodlayıcı başlatılır
                    val geocoder = android.location.Geocoder(getApplication(), Locale.getDefault())
                    // En kararlı API 26+ uyumluluğu gözetilerek maksimum 1 adres sonucu istenir
                    val addresses = geocoder.getFromLocation(lat, lon, 1)

                    if (!addresses.isNullOrEmpty()) {
                        // Önce şehir merkez adını (locality) al, null ise il/bölge adını (adminArea) al
                        val cityName = addresses[0].locality ?: addresses[0].adminArea ?: "Bilinmeyen Yer"
                        _locationName.value = cityName
                    } else {
                        // Eğer uydudan isim dönmezse koordinat moduna göre yedek isim ata
                        _locationName.value = if (lat == 51.9311) "Augustdorf" else "Konum Sabitlendi"
                    }
                } catch (e: Exception) {
                    // İnternet olmaması veya Geocoder sunucusunun gecikmesi durumunda çökmemesi için koruma kalkanı
                    _locationName.value = if (lat == 51.9311) "Augustdorf (Yedek)" else "Masa Saati"
                }
            }

            // Room veri tabanından bugünün vaktini sorgulayan akışı başlat
            getPrayerTimeUseCase(todayStr).collect { prayerTime ->
                if (prayerTime != null) {
                    _prayerTimes.value = prayerTime
                } else {
                    viewModelScope.launch(Dispatchers.IO) {
                        fetchPrayerTimesUseCase(lat, lon).onSuccess {
                            viewModelScope.launch(Dispatchers.Main) {
                                updateLiveTime()
                            }
                        }.onFailure { error ->
                            android.util.Log.e("MasaSaatim", "Sync Failed: ${error.localizedMessage}")
                        }
                    }
                }
            }
        }
    }


    /**
     * provideFactory: ViewModel'in AppContainer üzerinden güvenle enjekte edilerek başlatılmasını sağlar.
     */
    companion object {
        fun provideFactory(
            application: Application
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val container = (application as MainApplication).appContainer
                return MainViewModel(
                    application = application,
                    getPrayerTimeUseCase = container.getPrayerTimeUseCase,
                    fetchPrayerTimesUseCase = container.fetchPrayerTimesUseCase // DÜZELTİLDİ: Orijinal değişken adı bağlandı!
                ) as T
            }
        }
    }
}
