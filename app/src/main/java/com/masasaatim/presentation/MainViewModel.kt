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

class MainViewModel(
    application: Application,
    private val getPrayerTimeUseCase: GetPrayerTimeUseCase,
    private val prayerRepository: PrayerRepository
) : AndroidViewModel(application) {

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    // 🌟 LÖSUNG: Suppress-Annotation entfernt die unbenutzte Warnung dauerhaft
    @Suppress("unused")
    private val _prayerTimes = MutableStateFlow<PrayerTime?>(null)

    @Suppress("unused")
    val prayerTimes: StateFlow<PrayerTime?> = _prayerTimes.asStateFlow()

    private val _remainingTime = MutableStateFlow("00:00:00")
    val remainingTime: StateFlow<String> = _remainingTime.asStateFlow()

    private val _isDimmedMode = MutableStateFlow(false)
    val isDimmedMode: StateFlow<Boolean> = _isDimmedMode.asStateFlow()

    private val _isAzanPlaying = MutableStateFlow(false)
    val isAzanPlaying: StateFlow<Boolean> = _isAzanPlaying.asStateFlow()

    private val _locationName = MutableStateFlow("")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _pixelOffsetX = MutableStateFlow(0f)
    val pixelOffsetX: StateFlow<Float> = _pixelOffsetX.asStateFlow()

    private val _pixelOffsetY = MutableStateFlow(0f)
    val pixelOffsetY: StateFlow<Float> = _pixelOffsetY.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _nextVakitName = MutableStateFlow("")
    val nextVakitName: StateFlow<String> = _nextVakitName.asStateFlow()

    private val _currentConfig =
        MutableStateFlow(SavedLocation("Augustdorf", 51.9311, 8.8681, false))
    val currentConfig: StateFlow<SavedLocation> = _currentConfig.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateLiveTime()
            handler.postDelayed(this, 1000)
        }
    }

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
                                processManualLocationFetch(
                                    cityName,
                                    address.latitude,
                                    address.longitude
                                )
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
                // Secure ignore
            }
        }
    }

    private fun processManualLocationFetch(cityName: String, lat: Double, lon: Double) {
        val cleanCityName = cityName.trim().lowercase(Locale("tr")).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale("tr")) else it.toString()
        }

        viewModelScope.launch(Dispatchers.IO) {
            prayerRepository.fetchAndSaveRemotePrayerTimes(lat, lon).onSuccess {
                viewModelScope.launch(Dispatchers.Main) {
                    _currentConfig.value = SavedLocation(cleanCityName, lat, lon, false)
                    _locationName.value = cleanCityName
                    updateLiveTime()
                    setSettingsDialogVisible(false)
                }
            }
        }
    }

    fun updateLocationAutomatically(cityName: String, lat: Double, lon: Double) {
        if (_currentConfig.value.isAutomatic) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val realCityName = fetchCityNameSuspended(lat, lon) ?: cityName
                    executeRemoteDataSync(realCityName, lat, lon)
                } catch (_: Exception) {
                    // Secure ignore
                }
            }
        }
    }

    private suspend fun executeRemoteDataSync(realName: String, lat: Double, lon: Double) {
        prayerRepository.fetchAndSaveRemotePrayerTimes(lat, lon).onSuccess {
            viewModelScope.launch(Dispatchers.Main) {
                _currentConfig.value = SavedLocation(realName, lat, lon, true)
                _locationName.value = realName
                updateLiveTime()
            }
        }
    }

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

    private fun applyTemkinToTimeString(vakitName: String, timeStr: String): String {
        val diyanetTemkinPayi = mapOf(
            "imsak" to 39, "sunrise" to 1, "dhuhr" to 0, "asr" to 0, "maghrib" to 0, "isha" to -41
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
        } catch (_: Exception) {
            timeStr
        }
    }

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

        var nextVakit = "imsak"
        var nextVakitMs: Long = 0

        for (vakit in prayerList) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val vakitDate = sdf.parse(todayStr + vakit.second)

                if (vakitDate != null && vakitDate.time > currentMs) {
                    nextVakit = vakit.first
                    nextVakitMs = vakitDate.time
                    break
                }
            } catch (_: Exception) {
                // Secure ignore
            }
        }

        val turkishVakitName = when (nextVakit) {
            "imsak" -> "Imsak"
            "sunrise" -> "Gunes"
            "dhuhr" -> "Ogle"
            "asr" -> "Ikindi"
            "maghrib" -> "Aksam"
            "isha" -> "Yatsi"
            else -> "Imsak"
        }
        _nextVakitName.value = turkishVakitName

        if (nextVakitMs == 0L) {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowStr =
                SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(tomorrow.time)
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val imsakDate = sdf.parse(tomorrowStr + prayer.imsak)
                if (imsakDate != null) {
                    nextVakitMs = imsakDate.time
                    nextVakit = "imsak"
                }
            } catch (_: Exception) {
                // Secure ignore
            }
        }

        val diffMs = nextVakitMs - currentMs
        if (diffMs > 0) {
            if (diffMs <= 1000L && !_isAzanPlaying.value && nextVakit != "sunrise") {
                triggerAzanService(nextVakit)
            }

            val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMs) % 60
            _remainingTime.value =
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
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

        val calendar = Calendar.getInstance()
        val currentSecond = calendar.get(Calendar.SECOND)

        if (currentSecond == 0) {
            _pixelOffsetX.value = (-5..5).random().toFloat()
            _pixelOffsetY.value = (-5..5).random().toFloat()
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
                    address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: ""
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        _locationName.value = extractCity(addresses.firstOrNull())
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    _locationName.value = extractCity(addresses?.firstOrNull())
                }
            } catch (_: Exception) {
                // Secure ignore
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
            context.startForegroundService(intent)
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
        // 🌟 ÇÖZÜM: 'unused' bastırıcısı ile sarı çizgi kalıcı olarak kaldırıldı
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
