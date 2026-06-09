package com.masasaatim.domain.usecase

import com.masasaatim.domain.model.PrayerTime
import com.masasaatim.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow

/**
 * Sadece tek bir iş mantığından (Belirli bir tarihin ezan vakitlerini getirmek) sorumlu olan UseCase sınıfı.
 * ViewModel ile Veri Deposu (Repository) arasında köprü görevi görür.
 */
class GetPrayerTimeUseCase(private val repository: PrayerRepository) {

    /**
     * 'operator fun invoke' yapısı, bu sınıfın bir fonksiyon gibi çağrılabilmesini sağlar.
     * Yani ViewModel içinde "getPrayerTimeUseCase.invoke(date)" yazmak yerine
     * doğrudan "getPrayerTimeUseCase(date)" şeklinde daha temiz bir kullanım sunar.
     *
     * @param date Sorgulanacak olan tarih (Örn: "2026-06-09")
     * @return İlgili tarihin ezan vakitlerini içeren reaktif veri akışını (Flow) döndürür.
     */
    operator fun invoke(date: String): Flow<PrayerTime?> {
        return repository.getPrayerTimeForDate(date)
    }
}
