package com.masasaatim.core.network

import com.masasaatim.data.model.PrayerResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PrayerApiService {
    // Aylık vakitleri tek seferde çekerek internet ve pil tasarrufu sağlıyoruz
    @GET("v1/calendar")
    suspend fun getMonthlyPrayerTimes(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 13 // 13: Diyanet İşleri Başkanlığı metodu
    ): PrayerResponseDto
}
