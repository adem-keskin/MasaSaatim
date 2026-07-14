package com.masasaatim.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masasaatim.data.local.entity.PrayerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Veritabanı sorgularını ve işlemlerini barındıran Room DAO (Veri Erişim Nesnesi) arayüzü.
 * SQL sorguları yazmadan veya basit SQL şablonlarıyla veritabanı işlemlerini yapmayı sağlar.
 */
@Dao
interface PrayerDao {

    /**
     * Belirli bir tarihe ait ezan vakti kaydını veritabanından getirir.
     *
     * @Query -> SQL sorgusu çalıştırır. 'prayer_times' tablosundan, verilen tarihe eşit olan ilk kaydı seçer.
     * :date -> Fonksiyona gelen 'date' parametresini dinamik olarak SQL sorgusunun içine yerleştirir.
     * Flow<PrayerEntity?> -> Veriyi reaktif (canlı) bir akış olarak döner. Veritabanındaki bu veri
     * değiştiğinde, arayüz veya ilgili yerler otomatik olarak güncellenir. Kayıt yoksa null dönebilir.
     */
    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    fun getPrayerTimeByDate(date: String): Flow<PrayerEntity?>

    /**
     * Yeni ezan vakitleri listesini veritabanına toplu olarak kaydeder.
     *
     * @Insert -> Veritabanına veri ekleme işlemi yapar.
     * onConflict = OnConflictStrategy.REPLACE -> Eğer eklenmeye çalışılan bir tarihe ait kayıt
     * veritabanında zaten varsa (çakışma olursa), eski kaydı siler ve üzerine yenisini yazar.
     * suspend -> Bu işlem veritabanı diskiyle çalıştığı için bir Coroutine içinde (arka planda) yürütülür.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(prayerTimes: List<PrayerEntity>)

    /**
     * 🌟 YENİ: Kullanıcı şehir değiştirdiğinde eski verilerin temizlenmesini sağlar.
     * Tablodaki tüm verileri kalıcı olarak siler.
     */
    @Query("DELETE FROM prayer_times")
    suspend fun deleteAllPrayerTimes()

    /**
     * 🌟 YENİ: Veritabanında belirli bir aya ait veri olup olmadığını kontrol eder.
     * Örn: '2026-07%' formatında sorgulama yaparak internete çıkma ihtiyacını belirler.
     */
    @Query("SELECT COUNT(*) FROM prayer_times WHERE date LIKE :yearMonth || '%'")
    suspend fun getCountByMonth(yearMonth: String): Int
}
