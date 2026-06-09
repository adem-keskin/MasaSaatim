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
     * Verilen coğrafi koordinatlara (enlem ve boylam) göre aylık ezan vakitlerini sunucudan asenkron olarak indirir.
     *
     * @GET("v1/calendar") -> Ana internet adresinin (Base URL) sonuna "v1/calendar" yolunu ekleyerek bir GET isteği hazırlar.
     * suspend -> Bu fonksiyonun Coroutine içinde (arka planda) çalışacağını belirtir. Böylece internetten veri indirilirken ana ekran (UI) kilitlenmez.
     *
     * @param latitude Cihazdan veya haritadan alınan konumun enlem bilgisini URL'ye ekler (?latitude=...)
     * @param longitude Cihazdan veya haritadan alınan konumun boylam bilgisini URL'ye ekler (&longitude=...)
     * @param method Ezan vakti hesaplama yöntemini belirler. Varsayılan olarak 13 (Diyanet İşleri Başkanlığı) atanmıştır.
     *
     * @return Sunucudan gelen JSON yanıtını otomatik olarak 'PrayerResponseDto' veri modeline dönüştürüp geri döndürür.
     */
    @GET("v1/calendar")
    suspend fun getMonthlyPrayerTimes(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 13
    ): PrayerResponseDto
}
