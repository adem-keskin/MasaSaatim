package com.masasaatim.data.repository

// Ağ, veri tabanı, mimari katmanlar ve tarih işleme kütüphaneleri içe aktarılıyor.

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
 * PrayerRepositoryImpl Seniör Sınıfı: Domain katmanındaki 'PrayerRepository' arayüzünü (interface) uygular (implements).
 * Hem yerel veri tabanına (PrayerDao) hem de internet servisine (PrayerApiService) doğrudan erişebilen tek yerdir.
 */
class PrayerRepositoryImpl(
    private val prayerDao: PrayerDao,
    private val apiService: PrayerApiService
) : PrayerRepository {

    /**
     * getPrayerTimeForDate: Belirli bir tarihe ait namaz vakitlerini veri tabanından canlı olarak çeker.
     * Buradaki en önemli iş, veri tabanı modeli olan "PrayerEntity"yi, kullanıcı arayüzünün (UI)
     * anlayacağı saf temiz mimari modeli olan "PrayerTime" sınıfına dönüştürmektir (Mapping).
     */
    override fun getPrayerTimeForDate(date: String): Flow<PrayerTime?> {
        // Veri tabanından Flow akışı başlatılıyor
        return prayerDao.getPrayerTimeByDate(date).map { entity ->
            // Eğer veri tabanından o güne ait bir veri (entity) geldiyse dönüştür, gelmediyse null döndür
            entity?.let {
                // UI katmanının veri tabanı kütüphanelerinden (Room) bağımsız kalması için yeni nesne üretiliyor
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
     * fetchAndSaveRemotePrayerTimes: İnternetten namaz vakitlerini indirir, temizler ve diske kaydeder.
     *
     * @param latitude Kullanıcının enlem bilgisi
     * @param longitude Kullanıcının boylam bilgisi
     * @return Result<Unit> İşlemin başarılı (Success) veya başarısız (Failure) olduğunu bildiren sarmalayıcı yapı.
     */
    override suspend fun fetchAndSaveRemotePrayerTimes(
        latitude: Double,
        longitude: Double
    ): Result<Unit> {
        // withContext(Dispatchers.IO): Bu bloğun içindeki tüm internet ve diske yazma işlemlerini
        // ana işlemciyi (Main Thread) yormayacak şekilde, arka plandaki I/O (Giriş/Çıkış) iş parçacığına taşır.
        return withContext(Dispatchers.IO) {
            try {
                // 1. Adım: API sunucusundan (Diyanet metoduyla - 13) aylık veriler asenkron olarak indiriliyor.
                val response = apiService.getMonthlyPrayerTimes(latitude, longitude, 13)

                // 2. Adım: Sunucudan gelen karmaşık DTO listesi, veri tabanına yazılacak düzgün 'PrayerEntity' listesine dönüştürülüyor.
                val entities = response.dataList.map { day ->

                    // cleanTime: API'den bazen "05:12 (EEST)" gibi gelen saatlerin sonundaki ek bilgileri siler,
                    // sadece saf saat bilgisini ("05:12") filtreler ve boşlukları temizler (.trim()).
                    val cleanTime = { time: String -> time.substringBefore(" ").trim() }

                    // Tarih Dönüşüm Mekanizması:
                    // API'den gelen ham metin formatı belirleniyor (Örn: "05-06-2026")
                    val rawDate = day.dateInfo.gregorian.readableDate
                    val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                    // Veri tabanımızın (Room) indeksleme ve arama için beklediği standart ISO formatı belirleniyor (yyyy-MM-dd)
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                    // String tarih önce Date nesnesine parse ediliyor, hata oluşursa çökmemesi için günün tarihi (Date()) atanıyor.
                    val parsedDate = inputFormat.parse(rawDate) ?: Date()
                    // Güvenli hale gelen tarih "2026-06-05" formatına dönüştürülüyor.
                    val isoDateStr = outputFormat.format(parsedDate)

                    // Veri tabanı tablosuna eklenecek olan nihai satır nesnesi oluşturuluyor
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

                // 3. Adım: Temizlenen ve formata uygun hale getirilen tüm liste tek seferde veri tabanına yazılıyor.
                prayerDao.insertPrayerTimes(entities)

                // Geliştirici log ekranına işlemin başarıyla tamamlandığı bilgisi basılıyor.
                android.util.Log.d(
                    "MasaSaatim",
                    ">>> DATEN IM ISO-FORMAT (yyyy-MM-dd) GESPEICHERT <<<"
                )

                // İşlem hatasız bittiği için dış dünyaya başarı sinyali (Success) gönderiliyor.
                Result.success(Unit)
            } catch (e: Exception) {
                // İnternet kopması, sunucu çökmesi veya parse hatası durumunda hata loglara yazılıyor.
                android.util.Log.e("MasaSaatim", "API-Fehler: ${e.localizedMessage}")
                // Dış dünyaya işlemin başarısız olduğu ve hatanın ne olduğu (Failure) iletiliyor.
                Result.failure(e)
            }
        }
    }
}
