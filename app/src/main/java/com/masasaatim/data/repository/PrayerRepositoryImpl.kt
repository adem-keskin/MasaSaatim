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

/**
 * Domain katmanındaki PrayerRepository arayüzünü (interface) uygulayan somut sınıf.
 * Yerel veritabanı (Room) ve internet servisi (Retrofit) arasındaki veri akışını koordine eder.
 */
class PrayerRepositoryImpl(
    private val prayerDao: PrayerDao,       // Yerel veritabanı işlemlerini yürüten nesne
    private val apiService: PrayerApiService // İnternet isteklerini yürüten nesne
) : PrayerRepository {

    /**
     * Belirli bir tarihe ait ezan vakti kaydını yerel veritabanından canlı (Flow) olarak getirir.
     * Veritabanı modeli olan 'PrayerEntity' sınıfını, UI katmanının tanıdığı 'PrayerTime' iş modeline dönüştürür (Mapping).
     */
    override fun getPrayerTimeForDate(date: String): Flow<PrayerTime?> {
        return prayerDao.getPrayerTimeByDate(date).map { entity ->
            entity?.let {
                // Veritabanı veri nesnesi, Domain (İş) katmanı nesnesine haritalanıyor
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

    /**
     * Verilen coğrafi koordinatları kullanarak internetten aylık ezan vakitlerini indirir,
     * tarih/saat formatlarını temizler ve yerel Room veritabanına topluca kaydeder.
     */
    override suspend fun fetchAndSaveRemotePrayerTimes(
        latitude: Double,
        longitude: Double
    ): Result<Unit> {
        // Ağ (Network) ve disk (Database) işlemlerinin kullanıcı arayüzünü (UI) kilitlememesi için IO thread havuzuna geçilir
        return withContext(Dispatchers.IO) {
            try {
                // 1. Klasik donanım koordinatları ve Diyanet yöntemi (13) ile internetten veriler çekilir
                val response = apiService.getMonthlyPrayerTimes(latitude, longitude, 13)

                // 2. Gelen API modeli listesi (DTO), veritabanına kaydedilecek 'PrayerEntity' listesine dönüştürülür
                val entities = response.dataList.map { day ->
                    // Saat metinlerinin yanındaki parantez içi ek bilgileri (Örn: "04:12 (EEST)" -> "04:12") temizleyen yardımcı fonksiyon
                    val cleanTime = { time: String -> time.substringBefore(" ").trim() }

                    // --- TARİH FORMATLAMA İŞLEMLERİ ---
                    // API'den gelen "dd-MM-yyyy" (Örn: 09-06-2026) formatını alır
                    val rawDate = day.dateInfo.gregorian.readableDate
                    val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                    val outputFormat = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                    ) // Room için standart ISO formatı (yyyy-MM-dd)

                    // Metinsel tarihi Date nesnesine çevirir, başarısız olursa cihazın o anki tarihini baz alır
                    val parsedDate = inputFormat.parse(rawDate) ?: Date()

                    // Kapsam (Scope) hatası yaşanmaması için değişken tam burada güvenle deklare edilir:
                    val isoDateStr = outputFormat.format(parsedDate)

                    // Veritabanına yazılmaya hazır hale gelen kolon değerleri yapılandırılıyor
                    PrayerEntity(
                        date = isoDateStr, // Temizlenmiş ve standartlaştırılmış tarih anahtarı
                        imsak = cleanTime(day.timings.fajr),
                        gunes = cleanTime(day.timings.sunrise),
                        ogle = cleanTime(day.timings.dhuhr),
                        ikindi = cleanTime(day.timings.asr),
                        aksam = cleanTime(day.timings.maghrib),
                        yatsi = cleanTime(day.timings.isha)
                    )
                }

                // 3. Dönüştürülen tüm liste Room veritabanına tek seferde yazılır (Çakışma durumunda eskiler silinir ve üzerine yazılır)
                prayerDao.insertPrayerTimes(entities)
                android.util.Log.d("MasaSaatim", ">>> DATEN ERFOLGREICH IN ROOM GESPEICHERT <<<")

                Result.success(Unit) // İşlem bütünüyle başarılı oldu
            } catch (e: Exception) {
                // İnternet kesintisi veya herhangi bir sistem hatası durumunda hata loglanır ve başarısızlık durumu dönülür
                android.util.Log.e("MasaSaatim", "API-Fehler: ${e.localizedMessage}")
                Result.failure(e)
            }
        }
    }
}
