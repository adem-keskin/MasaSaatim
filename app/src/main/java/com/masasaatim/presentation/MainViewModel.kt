package com.masasaatim.presentation

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

    private val _remainingTime = MutableStateFlow("Berechnen...")
    val remainingTime: StateFlow<String> = _remainingTime.asStateFlow()

    // REAKTIVE KONTROLL-MOTOREN (OLED & AUDIO PROTECTION)
    private val _isDimmedMode = MutableStateFlow(false)
    val isDimmedMode: StateFlow<Boolean> = _isDimmedMode.asStateFlow()

    private val _isAzanPlaying = MutableStateFlow(false)
    val isAzanPlaying: StateFlow<Boolean> = _isAzanPlaying.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateLiveTime()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        handler.post(timeRunnable)
        loadPrayerData()
    }

    private fun updateLiveTime() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr")) // Türkischer Kalender-Text
        val now = Date()
        _currentTime.value = timeFormat.format(now)
        _currentDate.value = dateFormat.format(now)

        // AUTO-DIMMING ALGORITHMUS (22:00 - 06:00)
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val shouldDim = currentHour >= 22 || currentHour < 6
        if (_isDimmedMode.value != shouldDim) {
            _isDimmedMode.value = shouldDim
        }

        _prayerTimes.value?.let { calculateRemainingTime(it) }
    }

    private fun loadPrayerData() {
        viewModelScope.launch(Dispatchers.Main) {
            val todayStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

            getPrayerTimeUseCase(todayStr).collect { prayerTime ->
                if (prayerTime != null) {
                    _prayerTimes.value = prayerTime
                } else {
                    // Huawei/Asynchron-Sicherheitsnetz
                    viewModelScope.launch(Dispatchers.IO) {
                        fetchPrayerTimesUseCase(41.0082, 28.9784).onFailure { error ->
                            error.printStackTrace()
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
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowStr = SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(tomorrow.time)
            val imsakDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .parse(tomorrowStr + prayer.imsak)
            nextVakitMs = imsakDate?.time ?: 0L
            nextVakitName = "imsak"
        }

        val diff = nextVakitMs - currentMs

        // EXAKTER AUDIO-TIMER (Startet punktgenau zur 0. Sekunde)
        if (diff in 0..999 && currentMs % 60000 < 1500) {
            _isAzanPlaying.value = true
            triggerAzanService(nextVakitName)
        }

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

        _remainingTime.value = String.format("%s vaktine %02d:%02d:%02d kaldı", nextVakitName.uppercase(), hours, minutes, seconds)
    }

    private fun triggerAzanService(prayerType: String) {
        val intent = Intent(getApplication(), AzanPlaybackService::class.java).apply {
            putExtra(AzanPlaybackService.EXTRA_PRAYER_TYPE, prayerType.lowercase())
        }
        getApplication<Application>().startForegroundService(intent)
    }

    // AUDIO-SIMULATION (PLAY-ICON METODU)
    fun simulateAzanTrigger() {
        viewModelScope.launch {
            val currentVakit = _remainingTime.value.substringBefore(" ").lowercase()
            val targetVakit = if (currentVakit.contains("berechnen") || currentVakit.contains("kaldi")) "dhuhr" else currentVakit

            _isAzanPlaying.value = true
            triggerAzanService(targetVakit)
        }
    }

    // AUDIO-KILLER (STOP-ICON METODU)
    fun stopAzanPlayback() {
        viewModelScope.launch {
            _isAzanPlaying.value = false
            val intent = Intent(getApplication(), AzanPlaybackService::class.java).apply {
                action = AzanPlaybackService.ACTION_STOP_AZAN
            }
            getApplication<Application>().startForegroundService(intent)
        }
    }

    override fun onCleared() {
        handler.removeCallbacks(timeRunnable)
        super.onCleared()
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val container = (application as MainApplication).container
                return MainViewModel(
                    application,
                    container.getPrayerTimeUseCase,
                    container.fetchPrayerTimesUseCase
                ) as T
            }
        }
    }
}
