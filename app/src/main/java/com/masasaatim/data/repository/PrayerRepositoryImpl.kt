package com.masasaatim.data.repository

import com.masasaatim.core.network.PrayerApiService
import com.masasaatim.data.local.dao.PrayerDao
import com.masasaatim.data.local.entity.PrayerEntity
import com.masasaatim.domain.model.PrayerTime
import com.masasaatim.domain.repository.PrayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerRepositoryImpl(
    private val prayerDao: PrayerDao,
    private val apiService: PrayerApiService
) : PrayerRepository {

    override fun getPrayerTimeForDate(date: String): Flow<PrayerTime?> {
        return prayerDao.getPrayerTimeByDate(date).map { entity ->
            entity?.let {
                PrayerTime(
                    date = it.date,
                    imsak = it.imsak,
                    gunes = it.gunes,
                    ogle = it.ogle,
                    ikindi = it.ikindi,
                    aksam = it.aksam,
                    yatsi = it.yatsi
                )
            }
        }
    }

    override suspend fun fetchAndSaveRemotePrayerTimes(
        latitude: Double,
        longitude: Double
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMonthlyPrayerTimes(latitude, longitude, 13)
                val entities = response.dataList.map { day ->
                    val cleanTime = { time: String -> time.substringBefore(" ").trim() }

                    val rawDate = day.dateInfo.gregorian.readableDate
                    val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                    val parsedDate = inputFormat.parse(rawDate) ?: Date()
                    val isoDateStr = outputFormat.format(parsedDate)

                    PrayerEntity(
                        date = isoDateStr,
                        imsak = cleanTime(day.timings.fajr),
                        gunes = cleanTime(day.timings.sunrise),
                        ogle = cleanTime(day.timings.dhuhr),
                        ikindi = cleanTime(day.timings.asr),
                        aksam = cleanTime(day.timings.maghrib),
                        yatsi = cleanTime(day.timings.isha)
                    )
                }

                prayerDao.insertPrayerTimes(entities)
                android.util.Log.d("MasaSaatim", ">>> DATEN IM ISO-FORMAT (yyyy-MM-dd) GESPEICHERT <<<")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("MasaSaatim", "API-Fehler: ${e.localizedMessage}")
                Result.failure(e)
            }
        }
    }
}
