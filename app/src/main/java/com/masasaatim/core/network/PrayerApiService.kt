package com.masasaatim.core.network

import com.masasaatim.data.model.PrayerResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Aladhan API sunucusu ile iletişim kuran Retrofit ağ arayüzü (Interface).
 * Bu arayüz, HTTP isteklerini Kotlin fonksiyonlarına dönüştürerek internetten veri çekmeyi sağlar.
 */
interface PrayerApiService {

    /**
     * Verilen coğrafi koordinatlara ve zaman bilgilerine göre aylık ezan vakitlerini sunucudan asenkron olarak indirir.
     *
     * @GET("v1/calendar") -> Ana internet adresinin sonuna "v1/calendar" yolunu ekleyerek bir GET isteği hazırlar.
     * suspend -> Bu fonksiyonun Coroutine içinde (arka planda) çalışacağını belirtir, UI kilitlenmesini önler.
     *
     * @param latitude Cihazdan veya haritadan alınan konumun enlem bilgisi.
     * @param longitude Cihazdan veya haritadan alınan konumun boylam bilgisi.
     * @param month 🌟 YENİ: İstenen ay bilgisi (Örn: 7). Belirtilmezse sunucu mevcut ayı döndürür.
     * @param year 🌟 YENİ: İstenen yıl bilgisi (Örn: 2026). Belirtilmezse sunucu mevcut yılı döndürür.
     * @param method Ezan vakti hesaplama yöntemi. Varsayılan olarak 13 (Diyanet) atanmıştır.
     *
     * @return Sunucudan gelen JSON yanıtını otomatik olarak 'PrayerResponseDto' veri modeline dönüştürüp geri döndürür.
     */
    @GET("v1/calendar")
    suspend fun getMonthlyPrayerTimes(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("month") month: Int, // 🌟 Dinamik ay geçişleri için zorunlu hale getirdik
        @Query("year") year: Int,   // 🌟 Yıl geçişlerinde (Örn: Yılbaşı gecesi) veri kaybını önler
        @Query("method") method: Int = 13
    ): PrayerResponseDto
}
