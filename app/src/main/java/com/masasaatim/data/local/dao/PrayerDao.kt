package com.masasaatim.data.local.dao

// Room ve asenkron veri akışı (Flow) bileşenleri içe aktarılıyor.
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masasaatim.data.local.entity.PrayerEntity
import kotlinx.coroutines.flow.Flow

/**
 * @Dao: Room kütüphanesine bu arayüzün veri tabanı işlemlerini yürütecek
 * veri erişim sınıfı (Data Access Object) olduğunu bildirir.
 */
@Dao
interface PrayerDao {

    /**
     * getPrayerTimeByDate: Belirli bir tarihe ait namaz vakitlerini veri tabanından sorgular.
     *
     * @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1"):
     *      - "prayer_times" tablosundaki tüm sütunları seçer.
     *      - Dışarıdan gelen ":date" parametresi ile tablodaki "date" sütununu eşleştirir.
     *      - "LIMIT 1" ile performans için sadece tek bir satır veri getirmesini garanti eder.
     *
     * @return Flow<PrayerEntity?>: Kotlin Coroutines Flow yapısı kullanılmıştır.
     *      - Reaktiftir (Canlı Akış); veri tabanında o güne ait saatler değişirse arayüz (UI) kendini otomatik yeniler.
     *      - Arka planda (Asenkron) çalışır, ana ekranı dondurmaz.
     *      - Eğer girilen tarihte veri bulunamazsa "null" dönebilmesi için "?" eklenmiştir.
     */
    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    fun getPrayerTimeByDate(date: String): Flow<PrayerEntity?>

    /**
     * insertPrayerTimes: İnternetten indirilen aylık namaz vakitleri listesini veri tabanına topluca kaydeder.
     *
     * suspend fun: Bu işlemin diske yazma (I/O) operasyonu olduğunu ve işlemcinin ana iş parçacığını
     *              bloke etmemesi için arka planda (Asenkron) duraklatılabilir şekilde çalışacağını belirtir.
     *
     * onConflict = OnConflictStrategy.REPLACE: Çakışma stratejisidir. Eğer veri tabanına eklenmeye çalışılan
     *              tarih (PrimaryKey olan 'date') tabloda zaten mevcutsa, Room eski veriyi siler ve
     *              yerine güncel olan bu yeni veriyi yazar (Üzerine yazma/Upsert mantığı).
     *
     * @param prayerTimes Veri tabanına kaydedilecek olan günlük namaz vakitleri (Entity) listesi.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(prayerTimes: List<PrayerEntity>)
}
