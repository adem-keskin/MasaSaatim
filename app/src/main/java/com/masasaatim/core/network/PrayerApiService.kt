package com.masasaatim.core.network

// Projede kullanılacak veri transfer nesnesi (DTO) ve Retrofit bileşenleri içe aktarılıyor (import)

import com.masasaatim.data.model.PrayerResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * PrayerApiService Arayüzü: Namaz vakitlerini sunucudan çekmek için kullanılacak
 * tüm internet (API) isteklerini/fonksiyonlarını liste halinde tanımladığımız yerdir.
 */
interface PrayerApiService {

    /**
     * getMonthlyPrayerTimes: Belirli bir konuma ait aylık namaz vakitlerini sunucudan çeken fonksiyondur.
     *
     * @GET("v1/calendar"): Sunucu adresinin sonuna "/v1/calendar" yolunu ekleyerek bir HTTP GET isteği oluşturur.
     *                      (Örn: https://aladhan.com)
     *
     * suspend fun: Bu fonksiyonun "Asenkron" (Arka planda) çalışacağını belirtir. Ezan vakitleri internetten
     *              indirilirken uygulamanın arayüzünün (UI) donmasını veya kasmasını engeller.
     *
     * @Query: URL'in sonuna eklenecek dinamik parametreleri (Sorgu parametrelerini) ifade eder.
     *
     * @param latitude: Kullanıcının bulunduğu konumun Enlem (Latitude) bilgisini double türünde gönderir.
     * @param longitude: Kullanıcının bulunduğu konumun Boylam (Longitude) bilgisini double türünde gönderir.
     * @param method: Vakit hesaplama yönteminin kodudur. Varsayılan (default) olarak 13 atanmıştır.
     *                13 değeri, AlAdhan API standartlarında resmi T.C. Diyanet İşleri Başkanlığı takvimini temsil eder.
     *
     * @return PrayerResponseDto: Sunucudan gelen karmaşık JSON verisinin, Kotlin'de anlamlı bir nesne modeline
     *                            (Data Transfer Object - DTO) dönüştürülmüş halini geri döndürür.
     */
    @GET("v1/calendar")
    suspend fun getMonthlyPrayerTimes(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 13
    ): PrayerResponseDto
}
