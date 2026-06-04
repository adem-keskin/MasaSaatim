package com.masasaatim.presentation.service

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

class AzanPlaybackService : Service() {

    private var exoPlayer: ExoPlayer? = null

    companion object {
        const val EXTRA_PRAYER_TYPE = "EXTRA_PRAYER_TYPE"
        const val EXTRA_IS_DIMMED = "EXTRA_IS_DIMMED" // Gece modu anahtarı tanımlandı
        const val ACTION_STOP_AZAN = "ACTION_STOP_AZAN"
        const val NOTIFICATION_CHANNEL_ID = "ezan_channel"
        const val FOREGROUND_SERVICE_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_AZAN) {
            stopSelf()
            return START_NOT_STICKY
        }

        val prayerType = intent?.getStringExtra(EXTRA_PRAYER_TYPE) ?: "dhuhr"
        // Gece modu bilgisini oku (varsayılan olarak false)
        val isDimmed = intent?.getBooleanExtra(EXTRA_IS_DIMMED, false) ?: false

        val notificationText = if (isDimmed) "Adhan ($prayerType) wird im Nachtmodus leise abgespielt." else "Adhan ($prayerType) wird im Hintergrund abgespielt."

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Masa Saatim")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        try {
            startForeground(FOREGROUND_SERVICE_ID, notification)
            Handler(Looper.getMainLooper()).postDelayed({
                playAzanAudio(prayerType, isDimmed)
            }, 200)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun playAzanAudio(prayerType: String, isDimmed: Boolean) {
        val rawResourceId = when (prayerType.lowercase()) {
            "imsak" -> R.raw.imsak
            "sunrise" -> R.raw.ogle
            "dhuhr" -> R.raw.ogle
            "asr" -> R.raw.ikindi
            "maghrib" -> R.raw.aksam
            "isha" -> R.raw.yatsi
            else -> R.raw.ogle
        }

        val audioUri = "android.resource://$packageName/$rawResourceId".toUri()

        exoPlayer?.let { player ->
            player.setMediaItem(MediaItem.fromUri(audioUri))

            // --- SENIOR AKILLI SES FİLTRESİ ---
            // Eğer gece modundaysak ses seviyesini %20'ye (0.2f) çek, gündüz ise tam ses (1.0f) yap
            if (isDimmed) {
                player.volume = 0.2f
            } else {
                player.volume = 1.0f
            }
            // ----------------------------------

            player.prepare()
            player.play()

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        stopSelf()
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
