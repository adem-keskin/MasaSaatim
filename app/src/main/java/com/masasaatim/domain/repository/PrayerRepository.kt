package com.masasaatim.domain.repository

import com.masasaatim.domain.model.PrayerTime
import kotlinx.coroutines.flow.Flow

/**
 * Domain (İş Mantığı) katmanındaki veri deposu arayüzü (Repository Interface).
 * Verilerin tam olarak nereden geldiğiyle (API veya Veritabanı) ilgilenmez.
 * Sadece uygulamanın ihtiyaç duyduğu veri yeteneklerinin sınırlarını ve kurallarını belirler.
 */
interface PrayerRepository {

    /**
     * Belirli bir tarihe ait ezan vakitlerini yerel veri kaynağından canlı bir akış (Flow) olarak getirir.
     *
     * @param date Sorgulanacak olan standart tarih formatı (Örn: "2026-06-09")
     * @return İlgili tarihe ait temizlenmiş ezan vakti verilerini (PrayerTime) reaktif akış olarak döner. Kayıt yoksa null iletebilir.
     */
    fun getPrayerTimeForDate(date: String): Flow<PrayerTime?>

    /**
     * Cihazın donanımsal GPS uydularından gelen koordinatları (Enlem ve Boylam) kullanarak
     * internetten aylık güncel ezan vakitlerini indirir ve yerel hafızaya kaydeder.
     *
     * @param latitude Konumun harita üzerindeki enlem (Breitengrad) değeri (Double)
     * @param longitude Konumun harita üzerindeki boylam (Längengrad) değeri (Double)
     * @return İşlemin başarılı (Success) veya başarısız (Failure) olduğunu bildiren Kotlin 'Result' yapısı döner.
     */
    suspend fun fetchAndSaveRemotePrayerTimes(latitude: Double, longitude: Double): Result<Unit>
}
