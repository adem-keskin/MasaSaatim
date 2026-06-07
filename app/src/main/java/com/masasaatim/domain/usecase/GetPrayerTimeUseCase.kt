package com.masasaatim.domain.usecase

import com.masasaatim.domain.model.PrayerTime
import com.masasaatim.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow

class GetPrayerTimeUseCase(private val repository: PrayerRepository) {

    operator fun invoke(date: String): Flow<PrayerTime?> {
        return repository.getPrayerTimeForDate(date)
    }
}
