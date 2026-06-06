package com.masasaatim.domain.usecase

// Veri kaynaklarına erişmek için kullanılan soyut mimari arayüzü içe aktarılıyor (import).
import com.masasaatim.domain.repository.PrayerRepository

/**
 * FetchPrayerTimesUseCase Sınıfı: İnternetteki sunucudan koordinatlara göre namaz vakitlerini
 * senkronize etme (indirip yerel veri tabanına kaydetme) senaryosunu yürüten iş mantığı sınıfıdır.
 *
 * @param repository Veri yönetim merkezine (Repository) erişim sağlayan soyut arayüz bağımlılığı.
 */
class FetchPrayerTimesUseCase(private val repository: PrayerRepository) {

    /**
     * invoke: Kotlin'in operatör fonksiyonu esnekliği sayesinde bu sınıf, ViewModel içerisinde
     * nesne ismiyle doğrudan bir fonksiyon gibi (Örn: fetchPrayerTimesUseCase(lat, lon)) çağrılabilir.
     *
     * suspend: Bu işlem ağ (Network) ve disk yazma (I/O) operasyonlarını tetiklediği için,
     *          ana arayüzü (UI Thread) kilitlememesi adına duraklatılabilir arka plan iş parçacığı olarak işaretlenmiştir.
     *
     * @param latitude Kullanıcının bulunduğu konumun enlem bilgisi.
     * @param longitude Kullanıcının bulunduğu konumun boylam bilgisi.
     * @return Result<Unit> İşlemin internet kopması/sunucu hatası gibi durumlardan etkilenip etkilenmediğini
     *                      başarılı (Success) veya başarısız (Failure) şeklinde ViewModel'e güvenle bildiren sarmalayıcı yapı.
     */
    suspend operator fun invoke(latitude: Double, longitude: Double): Result<Unit> {
        // Talebi ve koordinatları veri yönetim merkezine (Repository) güvenli bir şekilde iletir.
        return repository.fetchAndSaveRemotePrayerTimes(latitude, longitude)
    }
}
