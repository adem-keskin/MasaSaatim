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

class AzanPlaybackService : Service() {

    private var exoPlayer: ExoPlayer? = null

    companion object {
        // Diese Konstanten entsprechen exakt den Aufrufen aus dem MainViewModel
        const val ACTION_START_AZAN = "START_AZAN"
        const val ACTION_STOP_AZAN = "STOP_AZAN"
        const val EXTRA_PRAYER_TYPE = "VAKIT_NAME"
        const val EXTRA_IS_DIMMED = "EXTRA_IS_DIMMED"

        // Huawei/SystemUI-kompatible ID ohne unzulässige Leerzeichen
        const val NOTIFICATION_CHANNEL_ID = "azan_service_channel"
        const val FOREGROUND_SERVICE_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
        createNotificationChannel() // Kanal wird sicher beim Start erstellt
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("MasaSaatimServis", "Gelen Action: $action")

        // Prüfen, ob der Stopp-Befehl gesendet wurde
        if (action == ACTION_STOP_AZAN) {
            Log.d("MasaSaatimServis", "Stopp-Befehl empfangen. Service wird beendet.")
            stopPlaybackAndService()
            return START_NOT_STICKY
        }

        val prayerType = intent?.getStringExtra(EXTRA_PRAYER_TYPE) ?: "dhuhr"
        val isDimmed = intent?.getBooleanExtra(EXTRA_IS_DIMMED, false) ?: false

        val notificationText = if (isDimmed) {
            "Adhan ($prayerType) wird im Nachtmodus leise abgespielt."
        } else {
            "Adhan ($prayerType) wird abgespielt."
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Masa Saatim")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Höhere Priorität gegen System-Filter
            .setOngoing(true)
            .build()

        try {
            startForeground(FOREGROUND_SERVICE_ID, notification)
            Handler(Looper.getMainLooper()).postDelayed({
                playAzanAudio(prayerType, isDimmed)
            }, 200)
        } catch (e: Exception) {
            Log.e("MasaSaatimServis", "Foreground-Start fehlgeschlagen: ${e.localizedMessage}")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun playAzanAudio(prayerType: String, isDimmed: Boolean) {
        // Zuweisung der Audiodateien aus res/raw
        val rawResourceId = when (prayerType.lowercase()) {
            "imsak" -> R.raw.imsak
            "sunrise" -> R.raw.ogle
            "dhuhr" -> R.raw.ogle
            "asr" -> R.raw.ikindi
            "maghrib" -> R.raw.aksam
            "yatsi" -> R.raw.yatsi
            else -> R.raw.ogle
        }

        val audioUri = "android.resource://$packageName/$rawResourceId".toUri()

        exoPlayer?.let { player ->
            player.stop() // Falls bereits etwas läuft, stoppen
            player.setMediaItem(MediaItem.fromUri(audioUri))
            player.volume = if (isDimmed) 0.2f else 1.0f
            player.prepare()
            player.play()

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        Log.d("MasaSaatimServis", "Wiedergabe beendet. Schließe Service.")
                        stopPlaybackAndService()
                    }
                }
            })
        }
    }

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

        // Benachrichtigung entfernen und Service komplett killen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "MasaSaatimAdhan" // Keine Leerzeichen oder Sonderzeichen im Bezeichner
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
                description = "Ezan Vakti Bildirim Servisi"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
