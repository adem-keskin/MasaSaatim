package com.masasaatim.presentation.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
 * Ezan vakti geldiğinde sistem arka planda veya cihaz uykudayken
 * ezan sesini kesintisiz çalan modern Android Ön Plan Servisi (Foreground Service).
 */
class AzanPlaybackService : Service() {

    private var exoPlayer: ExoPlayer? = null

    companion object {
        const val ACTION_START_AZAN = "START_AZAN"
        // 🌟 DÜZELTİLDİ: MainViewModel ile metinsel uyum sağlandı (Hatalı ACTION_ ön eki silindi)
        const val ACTION_STOP_AZAN = "STOP_AZAN"
        const val EXTRA_PRAYER_TYPE = "EXTRA_PRAYER_TYPE"
        const val EXTRA_IS_DIMMED = "EXTRA_IS_DIMMED"
        const val NOTIFICATION_CHANNEL_ID = "azan_service_channel"
        const val FOREGROUND_SERVICE_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        // Medya oynatıcı motorunu (ExoPlayer) hazırlar
        exoPlayer = ExoPlayer.Builder(this).build()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("AzanService", "Servis Tetiklendi, Aksiyon: $action")

        // Eğer kullanıcı arayüzden sustur butonuna bastıysa servisi kapatır
        if (action == ACTION_STOP_AZAN) {
            stopPlaybackAndService()
            return START_NOT_STICKY
        }

        // Gelen niyet (Intent) içindeki vakit adını ve loş mod durumunu çözer
        val prayerType = intent?.getStringExtra(EXTRA_PRAYER_TYPE) ?: "dhuhr"
        val isDimmed = intent?.getBooleanExtra(EXTRA_IS_DIMMED, false) ?: false

        // 🌟 DÜZELTİLDİ: Bildirim vitrin metinleri Türkçe yapıldı
        val notificationText = if (isDimmed) {
            "Ezan ($prayerType) gece modunda loş sesle okunuyor..."
        } else {
            "Ezan ($prayerType) okunuyor..."
        }

        // Kullanıcının kapatamayacağı kalıcı sistemi koruyan Ön Plan Bildirimi
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Masa Saatim")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        try {
            // 🌟 DÜZELTİLDİ: Android 14+ (API 34) ve Android 15 cihazlarda çökme yaşanmaması için
            // servisin medya oynatıcı türünde (MEDIA_PLAYBACK) çalıştığı sisteme bildirildi.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FOREGROUND_SERVICE_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(FOREGROUND_SERVICE_ID, notification)
            }

            // ExoPlayer'ın güvenli eşleşmesi için mikroskobik gecikmeyle oynatmayı tetikler
            Handler(Looper.getMainLooper()).postDelayed({
                playAzanAudio(prayerType, isDimmed)
            }, 200)
        } catch (_: Exception) {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    /**
     * Gelen vakit adına göre 'res/raw/' klasöründeki ses dosyasını ExoPlayer'a yükler ve oynatır.
     */
    private fun playAzanAudio(prayerType: String, isDimmed: Boolean) {
        // Vakit isimlerine göre doğru ses kaynağını eşleştiriyoruz (Örn: R.raw.imsak veya ogle)
        val rawResourceId = when (prayerType.lowercase()) {
            "imsak" -> R.raw.imsak
            "sunrise" -> R.raw.ogle // Güneş doğuşunda da öğle ezanı çalınacak şekilde atanmış
            "dhuhr" -> R.raw.ogle
            "asr" -> R.raw.ikindi
            "maghrib" -> R.raw.aksam
            "yatsi" -> R.raw.yatsi
            else -> R.raw.ogle
        }

        val audioUri = "android.resource://$packageName/$rawResourceId".toUri()

        exoPlayer?.let { player ->
            player.stop()
            player.setMediaItem(MediaItem.fromUri(audioUri))
            // Gece modu loş aktifse sesi %20 seviyesine, normalse %100 seviyesine ayarlar
            player.volume = if (isDimmed) { 0.2f } else { 1.0f }
            player.prepare()
            player.play()

            // Ses bittiğinde servisi ve bildirimi otomatik kapatacak dinleyici (Listener)
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        // MainViewModel'in ezanın bittiğini anlaması için sisteme yayın fırlatır
                        val statusIntent = Intent("com.masasaatim.AZAN_STATUS_CHANGED").apply {
                            putExtra("is_playing", false)
                        }
                        sendBroadcast(statusIntent)
                        stopPlaybackAndService()
                    }
                }
            })
        }
    }

    /**
     * Medya oynatıcıyı durdurur, sistemi temizler ve ön plan servisini kapatır.
     */
    private fun stopPlaybackAndService() {
        try {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
            }
        } catch (_: Exception) {
            // Güvenli pas geçme
        }

        // Bildirimi ekran şeridinden kaldırır ve servisi sonlandırır
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Android 8.0 (Oreo) sonrası zorunlu olan yüksek öncelikli bildirim kanalını kurar.
     */
    private fun createNotificationChannel() {
        val channelName = "Ezan Vakti Bildirimleri"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
            description = "Ezan Sesi Arka Plan Oynatıcı Servisi Kanalı"
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        // Bellek sızıntılarını önlemek için donanımsal ses işlemcisini serbest bırakır
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Bu servis Bound Servis olmadığı için null dönülür
    }
}
