package com.masasaatim.presentation.service

// Android sistem, medya oynatıcı (Media3) ve bildirim bileşenleri içe aktarılıyor.
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.masasaatim.R

/**
 * AzanPlaybackService Sınıfı: Namaz vakitlerinde arka planda ezan sesini oynatan Foreground (Ön Plan) servistir.
 * Ön plan servisi olduğu için bildirim çubuğunda kalıcı bir bildirim göstermek zorundadır;
 * bu sayede Android sistemi düşük bellek durumlarında bile bu servisi kolay kolay kapatamaz.
 */
class AzanPlaybackService : Service() {

    // Google Media3 kütüphanesinin güçlü ve modern medya oynatıcı nesnesi
    private var exoPlayer: ExoPlayer? = null

    companion object {
        // Servise dışarıdan (AlarmManager veya Activity'den) gönderilecek veri anahtarları (Intent Extra)
        const val EXTRA_PRAYER_TYPE = "EXTRA_PRAYER_TYPE" // Hangi vaktin ezanı okunacak? (Örn: "maghrib")
        const val EXTRA_IS_DIMMED = "EXTRA_IS_DIMMED"     // Masa saati gece/kısık modda mı? (%20 ses filtre anahtarı)
        const val ACTION_STOP_AZAN = "ACTION_STOP_AZAN"   // Ezanı dışarıdan (butona basarak) durdurma komutu

        // Bildirim kanalı ve servis kimlik numaraları (Android sistem gereksinimleri)
        const val NOTIFICATION_CHANNEL_ID = "ezan_channel"
        const val FOREGROUND_SERVICE_ID = 1001
    }

    /**
     * onCreate: Servis hafızada ilk kez oluşturulduğunda sadece bir kez tetiklenir.
     * Burada ExoPlayer istemcisini inşa ederek oynatım öncesi hazırlık yapıyoruz.
     */
    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
    }

    /**
     * onStartCommand: Servis her "startService(intent)" komutuyla uyandırıldığında burası tetiklenir.
     * Ezanın tetiklenme, bildirim hazırlama ve oynatıcıyı başlatma süreçlerini yönetir.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Eğer dışarıdan gelen emir ezanı durdurma (ACTION_STOP_AZAN) emriyse servisi kapat ve çık
        if (intent?.action == ACTION_STOP_AZAN) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Dışarıdan gelen veriler okunuyor. Veri gelmezse varsayılan değerler atanıyor.
        val prayerType = intent?.getStringExtra(EXTRA_PRAYER_TYPE) ?: "dhuhr"
        val isDimmed = intent?.getBooleanExtra(EXTRA_IS_DIMMED, false) ?: false

        // Gece moduna göre bildirim metni dinamik olarak Almanca dilinde ayarlanıyor
        val notificationText = if (isDimmed) {
            "Adhan ($prayerType) wird im Nachtmodus leise abgespielt."
        } else {
            "Adhan ($prayerType) wird im Hintergrund abgespielt."
        }

        // Android 8.0 ve üzeri için kalıcı (Ongoing) ön plan bildirimi inşa ediliyor
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Masa Saatim")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Bildirim çubuğundaki küçük ikon
            .setPriority(NotificationCompat.PRIORITY_LOW) // Kullanıcıyı sürekli sesle rahatsız etmeyen düşük öncelik
            .setOngoing(true) // Kullanıcının bildirimi sağa kaydırarak kapatmasını engeller
            .build()

        try {
            // Servisi resmi olarak "Foreground (Ön Plan)" statüsüne yükseltir.
            // İlk adımdaki manifest dosyanızda yazdığınız "foregroundServiceType=mediaPlayback" izni tam olarak burada devreye girer.
            startForeground(FOREGROUND_SERVICE_ID, notification)

            // Handler kullanarak 200 milisaniyelik çok küçük bir gecikmeyle ses oynatma fonksiyonunu çağırır.
            // Bu gecikme, servisin ön plana tamamen yerleşmesi ve işletim sisteminin kilitlenmemesi için güvenli bir yöntemdir.
            Handler(Looper.getMainLooper()).postDelayed({
                playAzanAudio(prayerType, isDimmed)
            }, 200)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf() // Herhangi bir güvenlik veya izin hatasında servisi güvenle kapatır
        }

        // START_NOT_STICKY: Sistem servisi beklenmedik şekilde kapatırsa,
        // ezanı yarıda tekrar başlatıp kullanıcıyı şaşırtmaması için servisi kendi haline bırakır.
        return START_NOT_STICKY
    }

    /**
     * playAzanAudio: Parametrelere göre doğru ses dosyasını bulur, ses seviyesini ayarlar ve oynatır.
     */
    private fun playAzanAudio(prayerType: String, isDimmed: Boolean) {
        // Gelen namaz vaktine göre "res/raw" klasörünüzdeki ilgili ses dosyasının (ID) seçimi yapılıyor
        val rawResourceId = when (prayerType.lowercase()) {
            "imsak" -> R.raw.imsak
            "sunrise" -> R.raw.ogle
            "dhuhr" -> R.raw.ogle
            "asr" -> R.raw.ikindi
            "maghrib" -> R.raw.aksam
            "yatsi" -> R.raw.yatsi // Kodunuzdaki harf uyuşmazlığı giderildi ("isha" yerine yerel entity standardınız "yatsi" yapıldı)
            else -> R.raw.ogle
        }

        // Seçilen raw ses dosyası, ExoPlayer'ın anlayacağı yerel bir URI adresine dönüştürülüyor
        val audioUri = "android.resource://$packageName/$rawResourceId".toUri()

        exoPlayer?.let { player ->
            // Oynatılacak medya öğesi yükleniyor
            player.setMediaItem(MediaItem.fromUri(audioUri))

            // --- SENIOR AKILLI SES FİLTRESİ ---
            // Eğer saat gece modundaysa (isDimmed) ses seviyesi %20'ye (0.2f) çekilir, normal ise tam ses (1.0f) verilir.
            if (isDimmed) {
                player.volume = 0.2f
            } else {
                player.volume = 1.0f
            }
            // ----------------------------------

            player.prepare() // Ses dosyasını arka planda çözerek oynatıma hazır hale getirir
            player.play()    // Ezanı oynatmaya başlar

            // ExoPlayer'ın durum değişikliklerini dinleyen dinleyici (Listener)
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    // Ezan sesi tamamen bittiğinde (STATE_ENDED) servisi otomatik olarak kapatır (stopSelf()).
                    // Böylece arka planda boşuna şarj ve işlemci tüketilmesi engellenir.
                    if (state == Player.STATE_ENDED) {
                        stopSelf()
                    }
                }
            })
        }
    }

    /**
     * onDestroy: Servis kapatıldığında (stopSelf veya harici komutla) tetiklenir.
     * Bellek sızıntılarını (Memory Leak) önlemek için ExoPlayer'ı tamamen serbest bırakır.
     */
    override fun onDestroy() {
        exoPlayer?.release() // Oynatıcının kullandığı tüm sistem kaynaklarını iade eder
        exoPlayer = null
        super.onDestroy()
    }

    // Bu servis başka bir Activity'ye bağlanmayacağı (Bound Service olmayacağı) için null döndürür.
    override fun onBind(intent: Intent?): IBinder? = null
}
