package com.masasaatim.core.network

import com.masasaatim.data.model.PrayerResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PrayerApiService {

    @GET("v1/calendar")
    suspend fun getMonthlyPrayerTimes(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 13, // Standardmäßig auf 13 (Diyanet) gesetzt
        @Query("calendarMethod") calendarMethod: String = "DIYANET" // Optimiert die Diyanet-Berechnung
    ): PrayerResponseDto
}
