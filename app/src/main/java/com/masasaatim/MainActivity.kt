package com.masasaatim

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.masasaatim.core.theme.DesktopClockTheme
import com.masasaatim.presentation.MainScreen
import com.masasaatim.presentation.MainViewModel
import kotlinx.coroutines.launch

// 🌟 DOĞRU IMPORT: Hatayı kökten çözen asıl kütüphane referansı
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * Uygulamanın ana ekran aktivitesi. Donanım izinlerini (GPS) ve
 * pencere yönetimini (Tam ekran, Yatay mod, Ekranın açık kalması) koordine eder.
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel // Ana iş mantığı kontrol merkezi
    private lateinit var fusedLocationClient: FusedLocationProviderClient // Google GPS Konum servisi istemcisi
    private var isLocationDataLoaded = false // Konumun başarıyla alınıp alınmadığını tutan bayrak

    /**
     * Donanım Tetikleyicisi: Cihazın GPS donanımı uydulara bağlanıp gerçek dünya
     * koordinatlarını (Enlem/Boylam) her yakaladığında bu tetikleyici ateşlenir.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location? = locationResult.lastLocation
            if (location != null) {
                Log.d("MasaSaatimGPS", "Gerçek donanım koordinatları alındı: Enlem: ${location.latitude}, Boylam: ${location.longitude}")
                isLocationDataLoaded = true

                viewModel.updateLocationAutomatically(
                    cityName = "Konum tespiti yapılıyor...",
                    lat = location.latitude,
                    lon = location.longitude
                )

                stopLocationUpdates()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasFineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val hasCoarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true
        }

        if (hasFineLocation || hasCoarseLocation) {
            startLocationUpdates()
        } else {
            Log.w("MasaSaatimGPS", "Konum izni reddedildi. Otomatik GPS kapatılıyor.")
            viewModel.toggleAutomaticLocation(false)
            startWithAugustdorfFallback()
        }

        if (!hasNotificationPermission) {
            Log.w("MasaSaatimGPS", "Bildirim izni reddedildi. Ezan sesleri arka planda duyulmayabilir!")
        }
    }
    private fun checkLocationPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        if ((fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED)
            && notificationPermission == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            val permissionsToRequest = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && notificationPermission != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🌟 ÇÖZÜM: Uzantı fonksiyonu (Extension function) doğru paketle çağrıldığı için
        // ComponentActivity üzerinde artık sıfır hata ile kusursuzca çalışır.
        installSplashScreen()

        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        viewModel = ViewModelProvider(this, MainViewModel.provideFactory(application))[MainViewModel::class.java]

        lifecycleScope.launch {
            viewModel.currentConfig.collect { config ->
                if (config.isAutomatic) {
                    checkLocationPermissions()
                } else {
                    stopLocationUpdates()
                }
            }
        }

        setContent {
            DesktopClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000)
                .setMinUpdateIntervalMillis(30000)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
            Log.d("MasaSaatimGPS", "GPS donanım anteni başarıyla aktif edildi.")
        } catch (unlikely: SecurityException) {
            Log.e("MasaSaatimGPS", "Başlatma esnasında güvenlik izni eksikliği: ${unlikely.localizedMessage}")
            startWithAugustdorfFallback()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("MasaSaatimGPS", "GPS donanım anteni kapatıldı (Pil tasarrufu).")
    }

    private fun startWithAugustdorfFallback() {
        if (!isLocationDataLoaded) {
            isLocationDataLoaded = true
            viewModel.loadPrayerDataWithLocation(51.9311, 8.8681)
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
}
