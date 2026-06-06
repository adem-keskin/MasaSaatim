package com.masasaatim

// Android sistem, donanım izinleri, pencere yöneticileri ve konum servisleri içe aktarılıyor.
import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.android.gms.location.LocationServices
import com.masasaatim.core.theme.DesktopClockTheme
import com.masasaatim.presentation.MainScreen
import com.masasaatim.presentation.MainViewModel

/**
 * MainActivity Sınıfı: Uygulamanın işletim sistemi tarafından tetiklenen ana ekranıdır.
 * Ekranın sürekli açık kalması, yatay mod kilitlenmesi, tam ekran immersif mod yönetimi
 * ve Google GPS konum servislerinin güvenli bir zaman aşımıyla başlatılması görevlerini üstlenir.
 */
class MainActivity : ComponentActivity() {

    // ViewModel katmanına erişim değişkeni
    private lateinit var viewModel: MainViewModel

    // Çift tetiklemeyi, asenkron yarış durumlarını (Race Condition) ve ekran donmalarını önleyen bayrak (Flag)
    private var isLocationDataLoaded = false

    /**
     * requestPermissionLauncher: Kullanıcıya konum izni sormak için kullanılan modern Android API arayüzüdür.
     * İzin verilirse veya reddedilirse tetiklenecek geri dönüş (Callback) mekanizmasını barındırır.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Eğer kullanıcı hassas (Hassas Konum) veya yaklaşık (Kaba Konum) izinlerinden en az birini onayladıysa
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getDeviceLocation() // Cihazın canlı GPS koordinatlarını çekmeye git
        } else {
            // İzin kullanıcı tarafından kesin olarak reddedilirse, uygulamayı kilitlemek yerine
            // doğrudan Almanya koordinatları ile güvenli (Fallback) modda başlat.
            startWithGermanyFallback()
        }
    }

    /**
     * onCreate: Aktivite ilk kez başlatıldığında tetiklenen ana yaşam döngüsü fonksiyonudur.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- MASA SAATİ DONANIM AYARLARI ---
        // FLAG_KEEP_SCREEN_ON: Masa saati standında dururken tabletin veya telefonun ekranının asla kapanmamasını sağlar.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // SCREEN_ORIENTATION_LANDSCAPE: Arayüz tasarımının bozulmaması için ekranı kalıcı olarak yatay moda sabitler.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // --- TAM EKRAN İMMERSİF MOD (IMMERSIVE MODE) AYARLARI ---
        // Pencerenin sistem çubuklarının arkasına taşmasına izin veriyoruz (Kenarlıksız ekran tasarımı)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            // Android'in üst bildirim çubuğunu (Status Bar) ve alt navigasyon çubuğunu (Navigation Bar) tamamen gizler
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE: Kullanıcı ekranı kenardan sürüklerse barlar geçici olarak gelir,
            // birkaç saniye sonra dokunulmazsa otomatik olarak tekrar gizlenir. Saat deneyimi bölünmez.
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // ViewModel nesnesi, MainApplication içindeki AppContainer bağımlılık fabrikası kullanılarak güvenle inşa ediliyor
        viewModel = ViewModelProvider(this, MainViewModel.provideFactory(application))[MainViewModel::class.java]

        // Konum izinlerinin daha önce verilip verilmediğini kontrol eden mekanizmayı tetikle
        checkLocationPermissions()

        // --- JETPACK COMPOSE UI GİYDİRME KATMANI ---
        setContent {
            DesktopClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen() // Tasarladığımız reaktif ana ekran arayüzünü buraya bağlıyoruz
                }
            }
        }
    }

    /**
     * checkLocationPermissions: Cihazda konum izninin durumunu kontrol eder.
     */
    private fun checkLocationPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        // Eğer izinlerden biri bile daha önceden onaylandıysa doğrudan konumu çekmeye başla
        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            getDeviceLocation()
        } else {
            // İzinler verilmediyse kullanıcıya resmi Android izin isteme penceresini (Pop-up) fırlat
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    /**
     * SENIOR TIMEOUT ENGINE (GPS Güvenlik Kilidi):
     * Google Fused Location API'sinin donanımsal veya sistemsel nedenlerle takılı kalma (hang)
     * problemini 3 saniyelik bir zaman aşımı (Timeout) mekanizması ile tamamen çözer.
     */
    private fun getDeviceLocation() {
        // ZAMAN AŞIMI GÜVENLİK DUVARI:
        // Eğer cihaz 3 saniye içinde GPS koordinatını döndüremezse, kilitlenmeyi kır ve Almanya Fallback motorunu aç.
        val timeoutHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!isLocationDataLoaded) {
                android.util.Log.w("MasaSaatim", "GPS yanıtı 3 saniye boyunca gelmedi. Almanya Fallback motoru zorla devreye alınıyor.")
                startWithGermanyFallback()
            }
        }
        // 3000 milisaniye (3 saniye) sonra çalışmak üzere Handler zamanlayıcısını başlatıyoruz
        timeoutHandler.postDelayed(timeoutRunnable, 3000)

        // Google Google Play Servisleri üzerinden konum istemcisini başlatıyoruz
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            // Cihazın önbelleğe alınmış en son bilinen konumunu talep ediyoruz
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                // Eğer bu satıra ulaşıldıysa konum yanıtı gelmiştir; Handler zaman aşımı sayacını hemen iptal et (Belleği koru)
                timeoutHandler.removeCallbacks(timeoutRunnable)

                // Konum başarıyla geldiyse ve daha önce veri yüklenmediyse
                if (location != null && !isLocationDataLoaded) {
                    isLocationDataLoaded = true
                    // ViewModel'e gerçek enlem ve boylamı göndererek internet/veri tabanı zincirini başlat
                    viewModel.loadPrayerDataWithLocation(location.latitude, location.longitude)
                } else if (!isLocationDataLoaded) {
                    // Konum servisi null döndüyse (Örn: GPS tamamen kapalıysa) Fallback motoruna sığın
                    startWithGermanyFallback()
                }
            }.addOnFailureListener {
                // Konum servisi donanımsal bir hata fırlatırsa zaman aşımını sil ve Fallback çalıştır
                timeoutHandler.removeCallbacks(timeoutRunnable)
                startWithGermanyFallback()
            }
        } catch (e: SecurityException) {
            // İzin eksikliği güvenlik istisnası durumunda çökmesini engelle ve Fallback çalıştır
            timeoutHandler.removeCallbacks(timeoutRunnable)
            e.printStackTrace()
            startWithGermanyFallback()
        }
    }

    /**
     * startWithGermanyFallback: GPS uydularının kapalı olması, izinlerin reddedilmesi veya zaman aşımı
     * durumlarında uygulamanın "Calculating..." ekranında kalmasını önleyen kararlı yedekleme motoru.
     */
    private fun startWithGermanyFallback() {
        if (!isLocationDataLoaded) {
            isLocationDataLoaded = true
            // Almanya (Augustdorf / NRW) Koordinatları: 51.9311 Enlem, 8.8681 Boylam
            // Bu sayede uygulama asla takılı kalmaz ve kararlı bir şekilde Almanya namaz vakitleriyle açılır.
            viewModel.loadPrayerDataWithLocation(51.9311, 8.8681)
        }
    }
}
