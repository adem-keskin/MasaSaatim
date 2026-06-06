package com.masasaatim

// Android uygulama tabanı, bildirim kanalları ve sistem servisleri içe aktarılıyor.
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.masasaatim.core.di.AppContainer

/**
 * MainApplication Sınıfı: Uygulamanın ömrü boyunca çalışan en üst düzey temel sınıftır.
 * Android manifest dosyanızdaki <application android:name=".MainApplication" ...> tanımı tam olarak bu sınıfı işaret eder.
 * Uygulama içi küresel nesnelerin (Konteynerler, Bildirim Kanalları vb.) tek bir kez başlatılması için en güvenli yerdir.
 */
class MainApplication : Application() {

    /**
     * appContainer: Manuel Bağımlılık Enjeksiyonu (Manual DI) mimarimizin kalbidir.
     * 'lateinit var' olarak tanımlanmıştır, yani uygulama ilk açıldığında belleğe alınır ve değeri atanır.
     * ViewModelFabrikası içerisinden (application as MainApplication).appContainer şeklinde çağrılan değişkendir.
     */
    lateinit var appContainer: AppContainer // ViewModel'deki çağrı ile senkronize olması için ismi 'appContainer' olarak düzeltildi.

    /**
     * onCreate: Uygulama işletim sistemi tarafından hafızaya yüklenirken İLK tetiklenen fonksiyondur.
     * Herhangi bir Activity (Ekran) veya Service (Arka plan işçisi) çalışmadan önce burası tamamlanır.
     */
    override fun onCreate() {
        super.onCreate()

        // 1. Adım: Tüm uygulamanın veri tabanı, internet ve usecase bağımlılıklarını tek merkezde topluyoruz.
        // Buraya 'this' (Application Context) gönderilerek Room veri tabanının diske güvenle yazılması sağlanır.
        appContainer = AppContainer(this)

        // 2. Adım: Ezan vakitlerinde arka plan servisinin işletim sistemine takılmadan çalışması için bildirim kanalını açıyoruz.
        createNotificationChannel()
    }

    /**
     * createNotificationChannel: Android 8.0 (Oreo) ve üzeri cihazlar için kalıcı bildirim kanalı oluşturur.
     * Android 8.0 sonrasında, her bildirim türünün (Ezan, mesaj, güncelleme vb.) sisteme kayıtlı bir kanalı olmak zorundadır.
     */
    private fun createNotificationChannel() {
        // Eğer cihazın Android sürümü Oreo (API 26) veya daha yeniyse bu blok çalışır.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Ezan bildirimlerinin sistem ayarlarında nasıl görüneceğini ve önem derecesini ekiyoruz.
            val channel = NotificationChannel(
                "ezan_channel",                      // Kanalın Benzersiz Kimliği (AzanPlaybackService içindeki NOTIFICATION_CHANNEL_ID ile TAM eşleşmelidir)
                "Ezan Vakti Bildirimleri",            // Kullanıcının telefon ayarlarında göreceği kanal ismi
                NotificationManager.IMPORTANCE_HIGH  // Yüksek önem derecesi: Ezan vaktinde ekranın üstünde belirmesini sağlar
            ).apply {
                // Kullanıcının kanal detaylarına girdiğinde okuyacağı açıklama metni
                description = "Ezan vakitlerinde sesli uyarı verir."
            }

            // Android sisteminin bildirim yöneticisi servisi çağrılıyor
            val manager = getSystemService(NotificationManager::class.java)

            // Oluşturduğumuz bu özel ezan kanalını resmi olarak işletim sistemine kaydediyoruz
            manager?.createNotificationChannel(channel)
        }
    }
}
