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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Android ana ekranında (Homescreen) ezan vakitlerini ve kalan süreyi
 * canlı gösteren masa saati widget sağlayıcısı.
 */
class MasaSaatiWidgetProvider : AppWidgetProvider() {

    // =========================================================================
    // 🌟 MERKEZİ TEMKİN DAKİKA AYARLARI
    // Süreleri değiştirmek istediğinizde sadece buradaki rakamları değiştirmeniz yeterlidir.
    // =========================================================================
    companion object {
        const val TEMKIN_IMSAK = 39   // İmsak vaktine 39 dakika ekler
        const val TEMKIN_GUNES = 1    // Güneş vaktine 1 dakika ekler
        const val TEMKIN_OGLE = 0     // Değişiklik yok
        const val TEMKIN_IKINDI = 0   // Değişiklik yok
        const val TEMKIN_AKSAM = 0    // Değişiklik yok
        const val TEMKIN_YATSI = -41  // Yatsı vaktinden 41 dakika çıkarır
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContainer = (context.applicationContext as MainApplication).appContainer
        val getPrayerTimeUseCase = appContainer.getPrayerTimeUseCase

        CoroutineScope(Dispatchers.IO).launch {
            val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = isoDateFormat.format(Date())
            val prayerTime = getPrayerTimeUseCase(todayStr).firstOrNull()

            withContext(Dispatchers.Main) {
                for (appWidgetId in appWidgetIds) {
                    val remoteViews = RemoteViews(context.packageName, R.layout.masa_saati_widget_layout)

                    remoteViews.setTextViewText(R.id.widget_location_name, "AUGUSTDORF")

                    if (prayerTime != null) {
                        // 🌟 DÜZELTİLDİ: Yukarıdaki merkezi temkin sabitleri buraya bağlandı
                        remoteViews.setTextViewText(R.id.widget_time_imsak, applyTemkin(TEMKIN_IMSAK, prayerTime.imsak))
                        remoteViews.setTextViewText(R.id.widget_time_gunes, applyTemkin(TEMKIN_GUNES, prayerTime.gunes))
                        remoteViews.setTextViewText(R.id.widget_time_ogle, applyTemkin(TEMKIN_OGLE, prayerTime.ogle))
                        remoteViews.setTextViewText(R.id.widget_time_ikindi, applyTemkin(TEMKIN_IKINDI, prayerTime.ikindi))
                        remoteViews.setTextViewText(R.id.widget_time_aksam, applyTemkin(TEMKIN_AKSAM, prayerTime.aksam))
                        remoteViews.setTextViewText(R.id.widget_time_yatsi, applyTemkin(TEMKIN_YATSI, prayerTime.yatsi))

                        // Geri sayım metni hesaplanıp basılıyor
                        val countdownText = calculateRemainingText(prayerTime)
                        remoteViews.setTextViewText(R.id.widget_countdown_timer, countdownText)
                    } else {
                        remoteViews.setTextViewText(R.id.widget_countdown_timer, "Yükleniyor...")
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

    /**
     * Diyanet takvimi uyumu için ezan vakitlerine dakika ekleyen veya çıkaran yardımcı fonksiyon.
     */
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

    /**
     * Sıradaki ezan vaktini bulup aradaki zaman farkını HH:mm:ss formatında metne dönüştürür.
     */
    private fun calculateRemainingText(prayer: PrayerTime): String {
        val now = Calendar.getInstance()
        val currentMs = now.timeInMillis
        val trLocale = Locale("tr", "TR")
        val todayStr = SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(Date())

        // 🌟 DÜZELTİLDİ: Sayaç matrisindeki isimler Türkçe yapıldı ve merkezi temkin süreleri buraya da tam eşitlendi!
        val prayerList = listOf(
            Pair("İmsak", applyTemkin(TEMKIN_IMSAK, prayer.imsak)),
            Pair("Güneş", applyTemkin(TEMKIN_GUNES, prayer.gunes)),
            Pair("Öğle", applyTemkin(TEMKIN_OGLE, prayer.ogle)),
            Pair("İkindi", applyTemkin(TEMKIN_IKINDI, prayer.ikindi)),
            Pair("Akşam", applyTemkin(TEMKIN_AKSAM, prayer.aksam)),
            Pair("Yatsı", applyTemkin(TEMKIN_YATSI, prayer.yatsi))
        )

        var nextVakit = "İmsak"
        var nextVakitMs: Long = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        for (vakit in prayerList) {
            try {
                val vakitDate = sdf.parse(todayStr + vakit.second)
                if (vakitDate != null && vakitDate.time > currentMs) {
                    nextVakit = vakit.first
                    nextVakitMs = vakitDate.time
                    break
                }
            } catch (_: Exception) { /* İhmal edildi */ }
        }

        if (nextVakitMs == 0L) {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowStr = SimpleDateFormat("yyyy-MM-dd ", Locale.getDefault()).format(tomorrow.time)
            try {
                val imsakDate = sdf.parse(tomorrowStr + applyTemkin(TEMKIN_IMSAK, prayer.imsak))
                if (imsakDate != null) {
                    nextVakitMs = imsakDate.time
                    nextVakit = "İmsak"
                }
            } catch (_: Exception) { /* İhmal edildi */ }
        }

        val diffMs = nextVakitMs - currentMs
        return if (diffMs > 0) {
            val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60

            String.format(trLocale, "%s: %02d:%02d", nextVakit, hours, minutes)
        } else {
            "00:00"
        }
    }
}
