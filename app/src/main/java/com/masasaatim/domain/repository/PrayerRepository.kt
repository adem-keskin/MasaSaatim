package com.masasaatim.domain.repository

// Tamamen bağımsız saf Kotlin modelleri ve asenkron akış bileşenleri içe aktarılıyor.
import com.masasaatim.domain.model.PrayerTime
import kotlinx.coroutines.flow.Flow

/**
 * PrayerRepository Arayüzü: Namaz vakitleri ile ilgili yapılacak tüm veri operasyonlarının
 * anayasasını (sözleşmesini) tanımlayan soyut mimari katmandır.
 * Bu arayüz, veri katmanındaki gerçek işi yapan 'PrayerRepositoryImpl' sınıfı tarafından doldurulur.
 */
interface PrayerRepository {

    /**
     * getPrayerTimeForDate: Belirli bir tarihe ait namaz vakitlerini getirmeyi taahhüt eder.
     *
     * @param date Sorgulanacak günün standart tarihi (yyyy-MM-dd, örn: "2026-06-05").
     * @return Flow<PrayerTime?> Kullanıcı arayüzünü besleyecek olan reaktif, canlı veri akışı.
     */
    fun getPrayerTimeForDate(date: String): Flow<PrayerTime?>

    /**
     * fetchAndSaveRemotePrayerTimes: İnternetteki sunucudan koordinatlara göre yeni vakitleri
     * indirmeyi ve yerel hafızaya güvenle yazmayı taahhüt eder.
     *
     * suspend: Bu işlem ağ ve disk operasyonlarını tetikleyeceği için arka planda asenkron çalışmak zorundadır.
     *
     * @param latitude Kullanıcının enlem bilgisi.
     * @param longitude Kullanıcının boylam bilgisi.
     * @return Result<Unit> İşlemin başarı durumunu (Success/Failure) sarmalayan güvenli Kotlin yapısı.
     */
    suspend fun fetchAndSaveRemotePrayerTimes(latitude: Double, longitude: Double): Result<Unit>
}
