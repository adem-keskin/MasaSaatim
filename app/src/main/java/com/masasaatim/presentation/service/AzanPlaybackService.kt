package com.masasaatim.presentation.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
        const val ACTION_START_AZAN = "START_AZAN"
        const val ACTION_STOP_AZAN = "ACTION_STOP_AZAN"
        const val EXTRA_PRAYER_TYPE = "EXTRA_PRAYER_TYPE"
        const val EXTRA_IS_DIMMED = "EXTRA_IS_DIMMED"
        const val NOTIFICATION_CHANNEL_ID = "azan_service_channel"
        const val FOREGROUND_SERVICE_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("Service", "Action: $action")

        if (action == ACTION_STOP_AZAN) {
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        try {
            startForeground(FOREGROUND_SERVICE_ID, notification)
            Handler(Looper.getMainLooper()).postDelayed({
                playAzanAudio(prayerType, isDimmed)
            }, 200)
        } catch (_: Exception) {
            // 🌟 LÖSUNG: Der Unterstrich (_) entfernt die Warnung über die ungenutzte Variable 'e'
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
            "yatsi" -> R.raw.yatsi
            else -> R.raw.ogle
        }

        val audioUri = "android.resource://$packageName/$rawResourceId".toUri()

        exoPlayer?.let { player ->
            player.stop()
            player.setMediaItem(MediaItem.fromUri(audioUri))
            player.volume = if (isDimmed) 0.2f else 1.0f
            player.prepare()
            player.play()

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
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

    private fun stopPlaybackAndService() {
        try {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
            }
        } catch (_: Exception) {
            // Error ignored safely
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        // 🌟 LÖSUNG: Alle redundanten Namen gekürzt und türkische Kommentare komplett entfernt
        val channelName = "Adhan"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
            description = "Adhan Notification Service"
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
