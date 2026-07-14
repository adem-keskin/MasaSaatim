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
     * Verilen coğrafi koordinatları, ay ve yıl bilgilerini kullanarak internetten aylık ezan vakitlerini indirir,
     * tarih/saat formatlarını temizler ve yerel Room veritabanına topluca kaydeder.
     */
    override suspend fun fetchAndSaveRemotePrayerTimes(
        latitude: Double,
        longitude: Double,
        month: Int,
        year: Int
    ): Result<Unit> {
        // Ağ (Network) ve disk (Database) işlemlerinin kullanıcı arayüzünü (UI) kilitlememesi için IO thread havuzuna geçilir
        return withContext(Dispatchers.IO) {
            try {
                // 1. ApiService arayüzüne ay ve yıl bilgileri güvenle paslanıyor
                val response = apiService.getMonthlyPrayerTimes(
                    latitude = latitude,
                    longitude = longitude,
                    month = month,
                    year = year,
                    method = 13
                )

                // 🌟 PERFORMANS OPTİMİZASYONU: Format nesnelerini ve yardımcı fonksiyonu döngünün dışına aldık.
                // Böylece 30 günlük listede her eleman için RAM'de tekrar tekrar nesne üretilmez.
                val cleanTime = { time: String -> time.substringBefore(" ").trim() }
                val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US) // Room için standart ISO formatı

                // 2. Gelen API modeli listesi (DTO), veritabanına kaydedilecek 'PrayerEntity' listesine dönüştürülür
                val entities = response.dataList.map { day ->
                    // API'den gelen "dd-MM-yyyy" (Örn: 13-07-2026) formatını alır
                    val rawDate = day.dateInfo.gregorian.readableDate

                    // Metinsel tarihi Date nesnesine çevirir, başarısız olursa cihazın o anki tarihini baz alır
                    val parsedDate = inputFormat.parse(rawDate) ?: Date()
                    val isoDateStr = outputFormat.format(parsedDate)

                    // Veritabanına yazılmaya hazır hale gelen kolon değerleri yapılandırılıyor
                    PrayerEntity(
                        date = isoDateStr, // Standartlaştırılmış tarih anahtarı (yyyy-MM-dd)
                        imsak = cleanTime(day.timings.fajr),
                        gunes = cleanTime(day.timings.sunrise),
                        ogle = cleanTime(day.timings.dhuhr),
                        ikindi = cleanTime(day.timings.asr),
                        aksam = cleanTime(day.timings.maghrib), // Model katmanıyla tam uyumlu alan
                        yatsi = cleanTime(day.timings.isha)
                    )
                }

                // 3. Dönüştürülen tüm liste Room veritabanına tek seferde yazılır
                prayerDao.insertPrayerTimes(entities)
                android.util.Log.d("MasaSaatim", ">>> EZAN VAKİTLERİ BAŞARIYLA ROOM VERİTABANINA KAYDEDİLDİ <<<")

                Result.success(Unit) // İşlem bütünüyle başarılı oldu
            } catch (e: Exception) {
                // Herhangi bir sistem hatası durumunda hata loglanır ve başarısızlık durumu dönülür
                android.util.Log.e("MasaSaatim", "API Hatası: ${e.localizedMessage}")
                Result.failure(e)
            }
        }
    }
}
