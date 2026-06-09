package com.masasaatim.presentation.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.masasaatim.R

/**
 * Ezan sesini arka planda kararlı bir şekilde oynatmak için kullanılan Ön Plan Servisi (Foreground Service).
 * 'Service' sınıfından türetilmiştir ve arayüzden bağımsız çalışır.
 */
class AzanPlaybackService : Service() {

    // Ses dosyalarını oynatmak için kullanılan modern Media3 ExoPlayer nesnesi
    private var exoPlayer: ExoPlayer? = null

    companion object {
        // Servisi başlatmak ve durdurmak için ViewModel tarafından gönderilen kontrol komutları (Actions)
        const val ACTION_START_AZAN = "START_AZAN"
        const val ACTION_STOP_AZAN = "STOP_AZAN"

        // Servise intent ile aktarılan ek veri anahtarları (Extras)
        const val EXTRA_PRAYER_TYPE = "VAKIT_NAME" // Hangi vaktin ezanı (Örn: imsak, ogle)
        const val EXTRA_IS_DIMMED = "EXTRA_IS_DIMMED" // Gece/Kısık mod aktif mi?

        // Android 8.0+ için zorunlu olan bildirim kanalı kimliği ve servis benzersiz numarası
        const val NOTIFICATION_CHANNEL_ID = "azan_service_channel"
        const val FOREGROUND_SERVICE_ID = 1001
    }

    /**
     * Servis hafızada ilk defa oluşturulurken tetiklenen yaşam döngüsü metodu.
     */
    override fun onCreate() {
        super.onCreate()
        // ExoPlayer nesnesi başlatılıyor
        exoPlayer = ExoPlayer.Builder(this).build()
        // Bildirim kanalı güvenli bir şekilde inşa ediliyor
        createNotificationChannel()
    }

    /**
     * Servis her başlatıldığında veya yeni bir komut (Intent) aldığında burası tetiklenir.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("MasaSaatimServis", "Gelen Action: $action")

        // Eğer gelen komut durdurma (STOP) komutu ise servisi ve ses oynatmayı tamamen kapatır
        if (action == ACTION_STOP_AZAN) {
            Log.d("MasaSaatimServis", "Stopp-Befehl empfangen. Service wird beendet.")
            stopPlaybackAndService()
            return START_NOT_STICKY // Servis kapandıktan sonra sistem tarafından zorla yeniden başlatılmasın
        }

        // Hangi vakit olduğunu ve gece modunda olup olmadığını Intent'ten oku (Yoksa varsayılan değerleri ata)
        val prayerType = intent?.getStringExtra(EXTRA_PRAYER_TYPE) ?: "dhuhr"
        val isDimmed = intent?.getBooleanExtra(EXTRA_IS_DIMMED, false) ?: false

        // Bildirim metnini gece moduna göre dinamik olarak belirle
        val notificationText = if (isDimmed) {
            "Adhan ($prayerType) wird im Nachtmodus leise abgespielt." // Gece modu aktifse leise (kısık) mesajı
        } else {
            "Adhan ($prayerType) wird abgespielt." // Normal mod mesajı
        }

        // Kullanıcıya zorunlu olarak gösterilecek olan ve sistemin servisi öldürmesini engelleyen bildirim kartı
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Masa Saatim")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Saat ikonu
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Sistem filtrelerine takılmamak için yüksek öncelik
            .setOngoing(true) // Kullanıcının bildirimi sağa kaydırarak silmesini engeller
            .build()

        try {
            // Servisi "Foreground" (Ön Plan) moduna geçir ve bildirimi göster
            startForeground(FOREGROUND_SERVICE_ID, notification)

            // Servis başlarken yaşanabilecek ufak çökmeleri önlemek için 200ms gecikmeli olarak sesi başlat
            Handler(Looper.getMainLooper()).postDelayed({
                playAzanAudio(prayerType, isDimmed)
            }, 200)
        } catch (e: Exception) {
            Log.e("MasaSaatimServis", "Foreground-Start fehlgeschlagen: ${e.localizedMessage}")
            stopSelf() // Hata durumunda servisi güvenli bir şekilde kapat
        }

        return START_NOT_STICKY
    }

    /**
     * Gelen vakit adına göre 'res/raw' klasöründeki ilgili ses dosyasını bulur ve oynatır.
     */
    private fun playAzanAudio(prayerType: String, isDimmed: Boolean) {
        // Gelen vakit ismine göre çalınacak mp3/wav dosyasını seçer
        val rawResourceId = when (prayerType.lowercase()) {
            "imsak" -> R.raw.imsak
            "sunrise" -> R.raw.ogle   // Güneş vaktinde varsayılan olarak öğle ezanı atanmış
            "dhuhr" -> R.raw.ogle
            "asr" -> R.raw.ikindi
            "maghrib" -> R.raw.aksam
            "yatsi" -> R.raw.yatsi
            else -> R.raw.ogle
        }

        // Ham ses dosyasının yolunu (URI) oluşturur
        val audioUri = "android.resource://$packageName/$rawResourceId".toUri()

        exoPlayer?.let { player ->
            player.stop() // Eğer halihazırda çalan bir ses varsa önce onu durdurur
            player.setMediaItem(MediaItem.fromUri(audioUri)) // Yeni ses dosyasını yükler

            // Gece modu aktifse sesi %10 seviyesine (0.1f) indirir, aktif değilse tam ses (1.0f) verir
            player.volume = if (isDimmed) 0.1f else 1.0f

            player.prepare() // Oynatıcıyı hazırlar
            player.play()    // Ezanı oynatmaya başlar

            // Ezan sesinin bitip bitmediğini dinleyen kulaklık (Listener)
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    // Ezan bittiğinde (STATE_ENDED) servisin arka planda boşuna şarj yememesi için kapatma komutunu tetikler
                    if (state == Player.STATE_ENDED) {
                        Log.d("MasaSaatimServis", "Wiedergabe beendet. Schließe Service.")
                        stopPlaybackAndService()
                    }
                }
            })
        }
    }

    /**
     * Ses oynatmayı durdurur, bildirimi kaldırır ve servisi sistemden tamamen temizler.
     */
    private fun stopPlaybackAndService() {
        try {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Ön plan bildirimini kaldır ve servisi sonlandır (Android sürüm kontrolü ile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf() // Servisi tamamen kapatır
    }

    /**
     * Android 8.0 (API 26) ve üzeri cihazlar için zorunlu olan Bildirim Kanalını (Notification Channel) oluşturur.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "MasaSaatimAdhan" // Sistem ayarlarında görünecek kanal ismi
            val importance = NotificationManager.IMPORTANCE_HIGH // Kilit ekranında da görünebilmesi için yüksek öncelik
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
                description = "Ezan Vakti Bildirim Servisi"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Servis tamamen yok edilirken (Bellekten atılırken) ExoPlayer'ı serbest bırakır.
     * Bu sayede bellek sızıntıları (Memory Leak) önlenmiş olur.
     */
    override fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    // Bu servis Bound Service (Bağlı Servis) olmadığı, doğrudan başlatıldığı (Started Service) için null dönülür.
    override fun onBind(intent: Intent?): IBinder? = null
}
