package com.masasaatim.presentation.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri // Die KTX-Erweiterungsfunktion für Strings
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.masasaatim.R

class AzanPlaybackService : Service() {

    private var exoPlayer: ExoPlayer? = null

    // Konstanten zur Vermeidung von String-Tippfehlern im Framework
    companion object {
        const val EXTRA_PRAYER_TYPE = "EXTRA_PRAYER_TYPE"
        const val NOTIFICATION_CHANNEL_ID = "ezan_channel"
        const val FOREGROUND_SERVICE_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Framework-Variablen komplett auf Englisch umgestellt
        val prayerType = intent?.getStringExtra(EXTRA_PRAYER_TYPE) ?: "dhuhr"

        // Lokalisierte Benachrichtigung (Deutsch), ohne riskante Sonderzeichen
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Masa Saatim")
            .setContentText("Adhan ($prayerType) wird im Hintergrund synchronisiert.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        try {
            startForeground(FOREGROUND_SERVICE_ID, notification)

            // Audio-Wiedergabe leicht verzögert starten, um Huawei EMUI zu entlasten
            Handler(Looper.getMainLooper()).postDelayed({
                playAzanAudio(prayerType)
            }, 200)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun playAzanAudio(prayerType: String) {
        // Zuordnung der Ressourcen komplett ohne länderspezifische Strings
        val rawResourceId = when (prayerType.lowercase()) {
            "imsak" -> R.raw.imsak
            "sunrise" -> R.raw.ogle // Falls benötigt, sonst Standard
            "dhuhr" -> R.raw.ogle
            "asr" -> R.raw.ikindi
            "maghrib" -> R.raw.aksam
            "isha" -> R.raw.yatsi
            else -> R.raw.ogle
        }

        // Elegant gelöst mit der KTX-Erweiterungsfunktion String.toUri()
        val audioUri = "android.resource://$packageName/$rawResourceId".toUri()

        exoPlayer?.let { player ->
            player.setMediaItem(MediaItem.fromUri(audioUri))
            player.prepare()
            player.play()

            // Ressourcen freigeben, wenn das Audio beendet ist
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
