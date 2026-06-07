package com.masasaatim.domain.repository

import com.masasaatim.domain.model.PrayerTime
import kotlinx.coroutines.flow.Flow

interface PrayerRepository {

    fun getPrayerTimeForDate(date: String): Flow<PrayerTime?>

    suspend fun fetchAndSaveRemotePrayerTimes(latitude: Double, longitude: Double): Result<Unit>
}
