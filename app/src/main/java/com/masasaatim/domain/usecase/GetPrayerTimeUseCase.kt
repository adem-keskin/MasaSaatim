package com.masasaatim.domain.usecase

// Domain katmanına ait bağımsız model ve arayüzler içe aktarılıyor (import).
// Bu katman, projedeki hiçbir veri tabanı (Room) veya internet (Retrofit) kütüphanesine doğrudan bağımlı değildir (Saf Kotlin).
import com.masasaatim.domain.model.PrayerTime
import com.masasaatim.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow

/**
 * GetPrayerTimeUseCase Sınıfı: Belirli bir güne ait namaz vakitlerini yerel veri tabanından
 * canlı akış (Flow) olarak çekme senaryosunu yürüten iş mantığı sınıfıdır.
 *
 * @param repository Veri kaynaklarına (Repository) erişmek için kullanılan soyut arayüz bağımlılığı.
 */
class GetPrayerTimeUseCase(private val repository: PrayerRepository) {

    /**
     * invoke: Kotlin'in özel bir operatör fonksiyonudur.
     * Bu sayede ViewModel içinde bu sınıfı çağırırken "getPrayerTimeUseCase.execute(date)" yazmak yerine,
     * doğrudan bir fonksiyonmuş gibi "getPrayerTimeUseCase(date)" şeklinde daha temiz ve okunabilir yazabilirsiniz.
     *
     * @param date Sorgulanmak istenen günün tarihi (yyyy-MM-dd formatında, örn: "2026-06-05").
     * @return Flow<PrayerTime?> Veri tabanından gelecek olan ve veri değiştikçe kendini güncelleyen reaktif namaz vakti modeli akışı.
     */
    operator fun invoke(date: String): Flow<PrayerTime?> {
        // Talebi doğrudan veri yönetim merkezine (Repository) yönlendirir.
        return repository.getPrayerTimeForDate(date)
    }
}
