package com.masasaatim.domain.usecase

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.masasaatim.domain.model.PrayerTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FetchPrayerTimesUseCase {

    /**
     * Berechnet die Gebetszeiten komplett offline über die stabile Adhan-Java-Bibliothek.
     * Kompatibel mit Kotlin 1.9.0 und berechnet präzise Diyanet-Werte.
     */
    operator fun invoke(latitude: Double, longitude: Double, targetDate: Date = Date()): Result<PrayerTime> {
        return try {
            val coordinates = Coordinates(latitude, longitude)
            val dateComponents = DateComponents.from(targetDate)

            // Adhan-Java 1.2.1 için resmi Türkiye/Diyanet metodunu çağırıyoruz
            //val parameters = CalculationMethod.TURKEY.parameters
            // Kütüphane enum hatası verirse bunu kullanın (Diyanet'in resmi açı ve temkin süreleridir):
            val parameters = com.batoulapps.adhan.CalculationParameters(18.0, 17.0).apply {
                methodAdjustments.fajr = 128
                methodAdjustments.sunrise = -6 // Diyanet temkin ayarı
                methodAdjustments.dhuhr = 5
                methodAdjustments.asr = 4
                methodAdjustments.maghrib = 6
                methodAdjustments.isha = -138
            }


            val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val mappedPrayerTime = PrayerTime(
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(targetDate),
                imsak = timeFormat.format(prayerTimes.fajr),
                gunes = timeFormat.format(prayerTimes.sunrise),
                ogle = timeFormat.format(prayerTimes.dhuhr),
                ikindi = timeFormat.format(prayerTimes.asr),
                aksam = timeFormat.format(prayerTimes.maghrib),
                yatsi = timeFormat.format(prayerTimes.isha)
            )

            Result.success(mappedPrayerTime)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
