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

class MainViewModel(
    application: Application,
    private val getPrayerTimeUseCase: GetPrayerTimeUseCase,
    private val fetchPrayerTimesUseCase: FetchPrayerTimesUseCase
) : AndroidViewModel(application) {

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

    private val _locationName = MutableStateFlow("Lade Standort...")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateLiveTime()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        handler.post(timeRunnable)

        Handler(Looper.getMainLooper()).postDelayed({
            if (_prayerTimes.value == null) {
                android.util.Log.d("MasaSaatim", "Açılışta veri tabanı boş çıktı. Otomatik senkronizasyon zorlanıyor...")
                loadPrayerDataWithLocation(41.0082, 28.9784)
            }
        }, 1500)
    }

    private fun updateLiveTime() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr"))
        val now = Date()
        _currentTime.value = timeFormat.format(now)
        _currentDate.value = dateFormat.format(now)

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val shouldDim = currentHour !in 6..21
        if (_isDimmedMode.value != shouldDim) {
            _isDimmedMode.value = shouldDim
        }

        _prayerTimes.value?.let { calculateRemainingTime(it) }
    }

    private fun fetchCityNameFromCoordinates(latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val geocoder = Geocoder(context, Locale("tr"))

                // Adres nesnesinden şehir ismini ayıklayan yardımcı fonksiyon
                val extractCity: (android.location.Address?) -> String = { address ->
                    // 1. Tercih: Doğrudan İlçe/Şehir (Örn: Augustdorf, Kadıköy)
                    // 2. Tercih: Büyükşehir/Merkez (Örn: Detmold, İstanbul)
                    // 3. Tercih: Hiçbiri bulunamazsa Eyalet/Bölge (Örn: Nordrhein-Westfalen)
                    address?.locality
                        ?: address?.subAdminArea
                        ?: address?.adminArea
                        ?: "Bilinmeyen Konum"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val city = extractCity(addresses.firstOrNull())
                        _locationName.value = city
                    }
                } else {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val city = extractCity(addresses?.firstOrNull())
                    _locationName.value = city
                }
            } catch (e: Exception) {
                android.util.Log.e("MasaSaatim", "Geocoder Hatası: ${e.localizedMessage}")
                _locationName.value = "Konum Alınamadı"
            }
        }
    }


    fun loadPrayerDataWithLocation(
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        if (latitude != null && longitude != null) {
            fetchCityNameFromCoordinates(latitude, longitude)
        }

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
            val tomorrow = Calendar.getInstance()
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowStr = SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(tomorrow.time)
            try {
                val imsakDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .parse(tomorrowStr + prayer.imsak)
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
                        appContainer.fetchPrayerTimesUseCase
                    ) as T
                }
            }
        }
    }
    // Funktion zum Auslösen des eigentlichen Hintergrunddienstes
    private fun triggerAzanService(vakitName: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _isAzanPlaying.value = true
            val context = getApplication<Application>().applicationContext
            val intent = Intent(context, AzanPlaybackService::class.java).apply {
                action = "START_AZAN"
                putExtra("VAKIT_NAME", vakitName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    // UI- oder Test-Button-Simulation
    fun simulateAzanTrigger() {
        android.util.Log.d("MasaSaatim", "Ezan çalma simülasyonu başlatıldı.")
        triggerAzanService("test_vakit")
    }

    // HIER IST DIE FEHLENDE FUNKTION:
    // Wird vom "Sustur"-Button im MainScreen aufgerufen
    fun stopAzanPlayback() {
        viewModelScope.launch(Dispatchers.Main) {
            _isAzanPlaying.value = false
            val context = getApplication<Application>().applicationContext
            val intent = Intent(context, AzanPlaybackService::class.java).apply {
                action = "STOP_AZAN"
            }
            context.startService(intent)
            android.util.Log.d("MasaSaatim", "Ezan servisine durdurma emri gönderildi.")
        }
    }
// HIER ENDET DIE KLASSE (Letzte Klammer der MainViewModel-Klasse)

}
