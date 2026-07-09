package com.masasaatim.presentation

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.masasaatim.MainActivity
import com.masasaatim.MainApplication
import com.masasaatim.R
import com.masasaatim.domain.model.PrayerTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MasaSaatiWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContainer = (context.applicationContext as MainApplication).appContainer
        val getPrayerTimeUseCase = appContainer.getPrayerTimeUseCase

        CoroutineScope(Dispatchers.IO).launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val prayerTime = getPrayerTimeUseCase(todayStr).firstOrNull()

            CoroutineScope(Dispatchers.Main).launch {
                for (appWidgetId in appWidgetIds) {
                    val remoteViews = RemoteViews(context.packageName, R.layout.masa_saati_widget_layout)

                    remoteViews.setTextViewText(R.id.widget_location_name, "AUGUSTDORF")

                    if (prayerTime != null) {
                        // 🌟 GÜNCELLEME: Tüm vakitler ve yeni eklenen Güneş vaktinin saatleri XML'e başarıyla giydirildi
                        remoteViews.setTextViewText(R.id.widget_time_imsak, applyTemkin(39, prayerTime.imsak))
                        remoteViews.setTextViewText(R.id.widget_time_gunes, applyTemkin(1, prayerTime.gunes))
                        remoteViews.setTextViewText(R.id.widget_time_ogle, applyTemkin(0, prayerTime.ogle))
                        remoteViews.setTextViewText(R.id.widget_time_ikindi, applyTemkin(0, prayerTime.ikindi))
                        remoteViews.setTextViewText(R.id.widget_time_aksam, applyTemkin(0, prayerTime.aksam))
                        remoteViews.setTextViewText(R.id.widget_time_yatsi, applyTemkin(-41, prayerTime.yatsi))

                        val countdownText = calculateRemainingText(prayerTime)
                        remoteViews.setTextViewText(R.id.widget_countdown_timer, countdownText)
                    } else {
                        remoteViews.setTextViewText(R.id.widget_countdown_timer, "Loading...")
                    }

                    val intent = Intent(context, MainActivity::class.java)
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

                    remoteViews.setOnClickPendingIntent(R.id.widget_location_name, pendingIntent)
                    remoteViews.setOnClickPendingIntent(R.id.widget_countdown_timer, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                }
            }
        }
    }

    private fun applyTemkin(minutes: Int, timeStr: String): String {
        if (minutes == 0) return timeStr
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = sdf.parse(timeStr) ?: return timeStr
            val cal = Calendar.getInstance().apply { time = date }
            cal.add(Calendar.MINUTE, minutes)
            sdf.format(cal.time)
        } catch (_: Exception) {
            timeStr
        }
    }

    private fun calculateRemainingText(prayer: PrayerTime): String {
        val now = Calendar.getInstance()
        val currentMs = now.timeInMillis
        val todayStr = SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(Date())

        // 🌟 GÜNCELLEME: Güneş vakti de canlı geri sayım zincirine dahil edildi
        val prayerList = listOf(
            Pair("Imsak", applyTemkin(39, prayer.imsak)),
            Pair("Gunes", applyTemkin(1, prayer.gunes)),
            Pair("Ogle", applyTemkin(0, prayer.ogle)),
            Pair("Ikindi", applyTemkin(0, prayer.ikindi)),
            Pair("Aksam", applyTemkin(0, prayer.aksam)),
            Pair("Yatsi", applyTemkin(-41, prayer.yatsi))
        )

        var nextVakit = "Imsak"
        var nextVakitMs: Long = 0

        for (vakit in prayerList) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val vakitDate = sdf.parse(todayStr + vakit.second)
                if (vakitDate != null && vakitDate.time > currentMs) {
                    nextVakit = vakit.first
                    nextVakitMs = vakitDate.time
                    break
                }
            } catch (_: Exception) { /* Ignored */ }
        }

        if (nextVakitMs == 0L) {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowStr = SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(tomorrow.time)
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val imsakDate = sdf.parse(tomorrowStr + applyTemkin(39, prayer.imsak))
                if (imsakDate != null) {
                    nextVakitMs = imsakDate.time
                    nextVakit = "Imsak"
                }
            } catch (_: Exception) { /* Ignored */ }
        }

        val diffMs = nextVakitMs - currentMs
        return if (diffMs > 0) {
            val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMs) % 60
            String.format(Locale.getDefault(), "%s: %02d:%02d:%02d", nextVakit, hours, minutes, seconds)
        } else {
            "00:00:00"
        }
    }
}
