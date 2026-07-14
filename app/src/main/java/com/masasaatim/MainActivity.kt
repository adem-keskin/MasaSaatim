package com.masasaatim

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

/**
 * Masa Saati uygulamasının donanımsal izinleri, GPS kontrollerini ve
 * tam ekran (Full Screen) sistem bileşenlerini yöneten ana giriş kapısı (Activity).
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isLocationDataLoaded = false

    // Donanımsal GPS uydularından gelen koordinat sonuçlarını dinleyen callback mekanizması
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location? = locationResult.lastLocation
            if (location != null) {
                isLocationDataLoaded = true
                // Gelen enlem ve boylamı ViewModel'e asenkron olarak iletir
                viewModel.updateLocationAutomatically(
                    cityName = "",
                    lat = location.latitude,
                    lon = location.longitude
                )
                stopLocationUpdates() // Konum alındıktan sonra pil harcamaması için dinleyiciyi kapatır
            }
        }
    }

    // Kullanıcıya gösterilen çoklu izin isteme penceresinin (Diyalog) sonuç dinleyicisi
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasFineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val hasCoarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasFineLocation || hasCoarseLocation) {
            startLocationUpdates()
        } else {
            // İzin verilmezse otomatik konumu kapatır ve varsayılan şehri (Augustdorf) yükler
            viewModel.toggleAutomaticLocation(false)
            startWithAugustdorfFallback()
        }
    }

    // Ezan çalan arka plan servisinden (ExoPlayer) gelen oynatma bitti durumlarını dinleyen alıcı
    private val azanStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isPlaying = intent?.getBooleanExtra("is_playing", false) ?: false
            viewModel.setAzanPlayingStatus(isPlaying)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // Android 12+ modern açılış ekranını başlatır
        super.onCreate(savedInstanceState)

        // Masa saati modunda ekranın asla kararıp kapanmamasını sağlar
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Uygulamayı yatay (Landscape) görünüm moduna kilitler
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Durum çubuğu ve navigasyon barını tamamen gizleyerek tam ekran (Immersive Mode) yapar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Konum ve ViewModel servis fabrikalarını bağlar
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        viewModel = ViewModelProvider(
            this,
            MainViewModel.provideFactory(application)
        )[MainViewModel::class.java]

        // ViewModel'deki konum ayarlarını canlı izleyerek GPS'i tetikler veya kapatır
        lifecycleScope.launch {
            viewModel.currentConfig.collect { config ->
                if (config.isAutomatic) {
                    checkLocationPermissions()
                } else {
                    stopLocationUpdates()
                }
            }
        }

        // Android 14+ kısıtlamalarına tam uyumlu ve dış sızıntılara kapalı güvenli alıcı tescili
        val listenFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        registerReceiver(
            azanStatusReceiver,
            IntentFilter("com.masasaatim.AZAN_STATUS_CHANGED"),
            listenFlags
        )

        // Jetpack Compose Arayüz Tasarımını ekrana basar
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

    /**
     * Gerekli konum ve Android 13+ bildirim izinlerinin verilip verilmediğini denetler.
     */
    private fun checkLocationPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        if ((fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED)
            && notificationPermission == PackageManager.PERMISSION_GRANTED
        ) {
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
    /**
     * Google Play Servislerini (FusedLocation) kullanarak yüksek doğrulukta GPS uydularını dinler.
     */
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
        } catch (_: SecurityException) {
            // Kullanıcı izin vermediyse veya güvenlik hatası oluşursa varsayılan yedek şehre döner
            startWithAugustdorfFallback()
        }
    }

    /**
     * GPS uydularını dinlemeyi durdurarak pil tasarrufu sağlar.
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * İnternet veya GPS uydularına erişilemediğinde Room veritabanını varsayılan yedek koordinatlarla besler.
     */
    private fun startWithAugustdorfFallback() {
        if (!isLocationDataLoaded) {
            isLocationDataLoaded = true
            // ViewModel metodu public yapıldığı için artık buradan sorunsuz çağrılır
            viewModel.loadPrayerDataWithLocation(51.9311, 8.8681)
        }
    }

    // 🌟 UYGULAMA ÖN PLANA GELDİĞİNDE: Otomatik konum modu açıksa GPS uydularını yeniden ateşler
    override fun onResume() {
        super.onResume()
        if (viewModel.currentConfig.value.isAutomatic) {
            checkLocationPermissions()
        }
    }

    // 🌟 UYGULAMA ARKA PLANA GEÇTİĞİNDE: Gereksiz pil tüketimini önlemek için GPS uydularını kapatır
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    // 🌟 UYGULAMA TAMAMEN KAPANDIĞINDA: Bellek sızıntılarını önlemek için alıcıyı sistemden düşer
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(azanStatusReceiver)
        } catch (_: Exception) {
            // Güvenli pas geçme
        }
    }
}
