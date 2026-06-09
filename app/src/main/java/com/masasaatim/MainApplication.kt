package com.masasaatim

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.masasaatim.core.di.AppContainer

/**
 * Uygulamanın en üst düzey (global) yaşam döngüsünü yöneten Application sınıfı.
 * AndroidManifest.xml dosyasında android:name=".MainApplication" olarak tanımlanmalıdır.
 */
class MainApplication : Application() {

    // Manuel Bağımlılık Enjeksiyonu (DI) konteynerimiz.
    // 'lateinit' ile tanımlanmıştır çünkü onCreate tetiklendiğinde başlatılacaktır.
    lateinit var appContainer: AppContainer

    /**
     * Uygulama işlem (process) seviyesinde ilk kez başlatılırken tetiklenen metot.
     * Herhangi bir Activity veya Service çalışmadan önce burası yürütülür.
     */
    override fun onCreate() {
        super.onCreate()

        // 1. Bağımlılık Konteynerini Başlatma:
        // Uygulama context'ini parametre olarak vererek tüm projenin veri tabanı,
        // API ve repository nesnelerini tek bir merkezde kurar.
        appContainer = AppContainer(this)

        // 2. Bildirim Kanalı Hazırlığı:
        // Cihaz başlatıldığında ezan bildirim kanallarının sisteme kayıtlı olmasını garantiler.
        createNotificationChannel()
    }

    /**
     * Android 8.0 (API 26) ve üzeri cihazlar için zorunlu olan Bildirim Kanalını sisteme kaydeder.
     * Bu kanal olmadan atılan bildirimler yeni Android sürümlerinde kullanıcıya gösterilmez.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // "ezan_channel" kimliğine (ID) sahip yüksek öncelikli bir kanal oluşturuluyor
            val channel = NotificationChannel(
                "ezan_service_channel",             // Sistem ayarlarında görünecek benzersiz kanal ID'si
                "Ezan Vakti Bildirimleri",    // Kullanıcının telefon ayarlarında göreceği kanal ismi
                NotificationManager.IMPORTANCE_HIGH // Kilit ekranında görünmesi ve ses çıkarması için yüksek öncelik
            ).apply {
                // Kullanıcının kanalın ne işe yaradığını anlaması için açıklama metni
                description = "Ezan vakitlerinde sesli uyarı verir."
            }

            // Android bildirim yöneticisi çağrılır ve hazırlanan kanal sisteme tescil edilir
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
