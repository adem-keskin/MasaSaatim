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

    private val _remainingTime = MutableStateFlow("Hesaplanıyor...")
    val remainingTime: StateFlow<String> = _remainingTime.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateLiveTime()
            handler.postDelayed(this, 1000) // Her saniye güncelle
        }
    }

    init {
        handler.post(timeRunnable)
        loadPrayerData()
    }

    private fun updateLiveTime() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr"))
        val now = Date()
        _currentTime.value = timeFormat.format(now)
        _currentDate.value = dateFormat.format(now)

        // Kalan süreyi her saniye güncelle
        _prayerTimes.value?.let { calculateRemainingTime(it) }
    }

    private fun loadPrayerData() {
        viewModelScope.launch(Dispatchers.Main) {
            val todayStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

            getPrayerTimeUseCase(todayStr).collect { prayerTime ->
                if (prayerTime != null) {
                    _prayerTimes.value = prayerTime
                } else {
                    // Arka plan iş parçacığında internetten çek ve uygulamayı çökertme
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

        val vakitler = listOf(
            Pair("imsak", prayer.imsak),
            Pair("ogle", prayer.ogle),
            Pair("ikindi", prayer.ikindi),
            Pair("aksam", prayer.aksam),
            Pair("yatsi", prayer.yatsi)
        )

        var nextVakitName = "İmsak"
        var nextVakitMs: Long = 0

        for (vakit in vakitler) {
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

        // HUAWEİ GÜVENLİK FİLTRESİ: Sadece tam saniyesinde (0. saniyede) tetiklenmesini sağla.
        // İlk açılışta milisaniyelik gecikmeler yüzünden servisin çökmesini engeller.
        if (diff in 0..999 && currentMs % 60000 < 1500) {
            triggerAzanService(nextVakitName)
        }

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

        _remainingTime.value = String.format("%s vaktine %02d:%02d:%02d kaldı", nextVakitName.uppercase(), hours, minutes, seconds)
    }


    private fun triggerAzanService(vakitName: String) {
        val intent = Intent(getApplication(), AzanPlaybackService::class.java).apply {
            // Nutzt jetzt die saubere Framework-Konstante des Services
            putExtra(AzanPlaybackService.EXTRA_PRAYER_TYPE, vakitName.lowercase())
        }
        getApplication<android.app.Application>().startForegroundService(intent)
    }

    override fun onCleared() {
        handler.removeCallbacks(timeRunnable)
        super.onCleared()
    }

    // Senior standardında Factory yapısı ile ViewModel üretimi
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
