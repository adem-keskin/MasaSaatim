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

/**
 * Uygulamanın ana ekran aktivitesi. Donanım izinlerini (GPS) ve
 * pencere yönetimini (Tam ekran, Yatay mod, Ekranın açık kalması) koordine eder.
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel // Ana iş mantığı kontrol merkezi
    private lateinit var fusedLocationClient: FusedLocationProviderClient // Google GPS Konum servisi istemcisi
    private var isLocationDataLoaded = false // Konumun başarıyla alınıp alınmadığını tutan bayrak

    /**
     * Hardware-Callback: Cihazın GPS donanımı uydulara bağlanıp gerçek dünya
     * koordinatlarını (Enlem/Boylam) her yakaladığında bu tetikleyici ateşlenir.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location? = locationResult.lastLocation
            if (location != null) {
                Log.d("MasaSaatimGPS", "Echte Hardware-Koordinaten empfangen: Lat: ${location.latitude}, Lon: ${location.longitude}")
                isLocationDataLoaded = true

                // Yakalanan enlem ve boylam değerleri otomatik işlenmesi için ViewModel'e gönderilir
                viewModel.updateLocationAutomatically(
                    cityName = "Ort wird ermittelt...", // Şehir ismi arka planda Geocoder ile çözülecek
                    lat = location.latitude,
                    lon = location.longitude
                )

                // Pil Tasarrufu Modu: Konum bir kez başarıyla alındıktan sonra
                // bataryayı tüketmemek için GPS taraması durdurulur.
                stopLocationUpdates()
            }
        }
    }

    /**
     * İzin İsteme Motoru (Launcher): Konum izinleri ile birlikte Android 13+ bildirim
     * iznini tek bir seferde asenkron olarak talep eden modern API mekanizması.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 1. Konum İzinlerinin Kontrolü
        val hasFineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val hasCoarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        // 2. Yeni Eklenen Bildirim İzninin Kontrolü (Android 13 ve üzeri için)
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true // Android 13 altı cihazlarda bu izin varsayılan olarak kabul edilir
        }

        // Konum izinlerinden en az biri verildiyse GPS donanımını ateşle
        if (hasFineLocation || hasCoarseLocation) {
            startLocationUpdates()
        } else {
            Log.w("MasaSaatimGPS", "Konum izni reddedildi. Otomatik GPS kapatılıyor.")
            viewModel.toggleAutomaticLocation(false)
            startWithGermanyFallback()
        }

        // Bildirim izni durumunu takip etme (Hata ayıklama için log atar)
        if (!hasNotificationPermission) {
            Log.w("MasaSaatimGPS", "Bildirim izni reddedildi. Ezan sesleri arka planda duyulmayabilir!")
        }
    }

    /**
     * Cihazın konum ve bildirim izinlerini analiz eden güncel kontrol fonksiyonu.
     * ViewModel'deki 'isAutomatic' tetiklendiğinde doğrudan bu fonksiyon çağrılır.
     */
    private fun checkLocationPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        // Android 13+ cihazlar için bildirim izni durumunu sorgula
        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        // Eğer tüm izinler (Konum ve Bildirim) zaten verilmişse doğrudan GPS'i başlat
        if ((fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED)
            && notificationPermission == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            // İzinlerden biri bile eksikse, istenecek izin listesini hazırla
            val permissionsToRequest = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            // Eğer cihaz Android 13+ ise ve bildirim izni eksikse listeye ekle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && notificationPermission != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            // Hazırlanan izin listesini kullanıcıya gösterilmek üzere fırlat
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- SİSTEMİK DONANIM VE PENCERE YAPILANDIRMASI ---

        // Ekran Koruyucu Engelleyici: Cihaz masa üstünde açık dururken ekranın kararmasını veya kapanmasını kesin olarak önler.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Yatay Ekran Zorunluluğu: Cihaz dik tutulsa bile arayüzün kalıcı olarak yatay (Landscape) kalmasını sağlar.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Tam Ekran Yapılandırması: Arayüz elemanlarının sistem çubuklarının arkasına da taşarak gerçek tam ekran olmasını sağlar.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            // Durum çubuğu (Status Bar) ve gezinme çubuğunu (Navigation Bar) tamamen gizler.
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // Kullanıcı ekranı kenardan kaydırınca barlar geçici görünür, dokunmayı bırakınca otomatik geri kapanır.
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Google Fused Location API istemcisi cihaz için ilk kez hazırlanıyor
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Custom Factory (Uygulama DI Konteyneri) aracılığıyla MainViewModel örneği bağımlılıkları ile birlikte güvenle oluşturuluyor
        viewModel = ViewModelProvider(this, MainViewModel.provideFactory(application))[MainViewModel::class.java]

        // --- REAKTİF KNOTENPUNKT: GPS DİNLEME MERKEZİ ---
        // Ayarlar menüsündeki "Otomatik Konum (GPS)" anahtarının durumunu eşzamanlı (asenkron) olarak takip eder.
        lifecycleScope.launch {
            viewModel.currentConfig.collect { config ->
                if (config.isAutomatic) {
                    // Kullanıcı arayüzden GPS anahtarını açtıysa runtime izin kontrollerini ve aramasını başlatır
                    checkLocationPermissions()
                } else {
                    // Kullanıcı el ile şehir moduna geçtiyse donanım GPS antenini kapatır (Müthiş bir güç tasarrufu)
                    stopLocationUpdates()
                }
            }
        }

        // --- JETPACK COMPOSE ARAYÜZÜNÜN YÜKLENMESİ ---
        setContent {
            DesktopClockTheme {
                // Tüm ekranı kaplayan ve temadaki arka plan rengini (saf siyah) alan ana yüzey bileşeni
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen() // Tasarladığımız ana ekran motorunu çalıştırır
                }
            }
        }
    }

    /**
     * Google Fused Location altyapısını kullanarak donanımsal GPS taramasını başlatan fonksiyon.
     */
    private fun startLocationUpdates() {
        try {
            // Yüksek doğruluk modu (PRIORITY_HIGH_ACCURACY) talep ediliyor.
            // Pil ömrünü korumak adına güncellemeler her 60 saniyede (60000ms) bir olacak şekilde planlanıyor.
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000)
                .setMinUpdateIntervalMillis(30000) // En hızlı iki güncelleme arası süreyi 30 saniyeye sınırlar
                .build()

            // Belirlenen kriterler, konum tetikleyicisi (locationCallback) ve ana ekran döngüsü (mainLooper) ile GPS istemcisine bağlanır.
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
            Log.d("MasaSaatimGPS", "GPS-Hardware-Antenne erfolgreich aktiviert.")
        } catch (unlikely: SecurityException) {
            // Kullanıcı izinleri o esnada işletim sistemi ayarlarından aniden kapattıysa çökme olmaması için bu catch bloğu çalışır
            Log.e("MasaSaatimGPS", "Fehlende Sicherheitsberechtigung beim Start: ${unlikely.localizedMessage}")
            startWithGermanyFallback() // Güvenli yedek konumu devreye sok
        }
    }

    /**
     * Aktif olan GPS donanım dinleyicisini kapatan fonksiyon.
     * Cihazın bataryasının gereksiz yere tükenmesini (şarj yemesini) engeller.
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("MasaSaatimGPS", "GPS-Hardware-Antenne deaktiviert (Batterieschonung).")
    }

    /**
     * GPS uydularına erişilemediğinde veya kullanıcı izin vermediğinde uygulamanın
     * tamamen verisiz/boş kalmasını önleyen Almanya/Augustdorf yedek konum motoru.
     */
    private fun startWithGermanyFallback() {
        // Eğer şu ana kadar hiçbir konum verisi başarıyla yüklenemediyse devreye girer
        if (!isLocationDataLoaded) {
            isLocationDataLoaded = true
            // Varsayılan olarak projenin ana merkezi olan Almanya koordinatlarını ViewModel'e yükletir
            viewModel.loadPrayerDataWithLocation(51.9311, 8.8681)
        }
    }

    /**
     * Android yaşam döngüsü metodudur. Uygulama simge durumuna küçültüldüğünde,
     * kilitlendiğinde veya başka bir uygulamaya geçildiğinde tetiklenir.
     */
    override fun onPause() {
        super.onPause()
        // Kullanıcı uygulamayı ekrandan kaldırdığı an GPS antenini derhal keserek pil tasarrufu sağlar.
        stopLocationUpdates()
    }
} // MainActivity Sınıfının Sonu
