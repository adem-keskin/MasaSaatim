package com.masasaatim.domain.usecase

import com.masasaatim.domain.repository.PrayerRepository

class FetchPrayerTimesUseCase(private val repository: PrayerRepository) {

    suspend operator fun invoke(latitude: Double, longitude: Double): Result<Unit> {
        return repository.fetchAndSaveRemotePrayerTimes(latitude, longitude)
    }
}
