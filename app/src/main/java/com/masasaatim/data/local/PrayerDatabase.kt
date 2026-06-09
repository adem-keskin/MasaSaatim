package com.masasaatim.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.masasaatim.data.local.dao.PrayerDao
import com.masasaatim.data.local.entity.PrayerEntity

/**
 * Room Veritabanı yapılandırma sınıfı.
 *
 * @Database anotasyonu ile veritabanına dahil edilecek tablolar (entities) ve versiyon numarası belirtilir.
 * exportSchema = false -> Veritabanı şemasının dışarıya bir JSON dosyası olarak aktarılmasını engeller.
 */
@Database(entities = [PrayerEntity::class], version = 1, exportSchema = false)
abstract class PrayerDatabase : RoomDatabase() {

    // Veritabanı üzerinden sorguları çalıştıracağımız DAO arayüzünü dışarıya açar.
    abstract fun prayerDao(): PrayerDao

    companion object {
        /**
         * @Volatile anotasyonu, bu değişkenin değerinde yapılan değişikliklerin
         * tüm işlemci çekirdekleri (threads) tarafından anında görünür olmasını sağlar.
         * Veritabanının hafızada mükerrer (birden fazla) oluşturulmasını engeller.
         */
        @Volatile
        private var INSTANCE: PrayerDatabase? = null

        /**
         * Veritabanı nesnesini getiren fonksiyon (Singleton Kalıbı).
         * Eğer veritabanı daha önce oluşturulmuşsa mevcut olanı döner, oluşturulmamışsa sıfırdan kurar.
         */
        fun getDatabase(context: Context): PrayerDatabase {
            // Eğer INSTANCE null değilse doğrudan döndür, null ise senkronize bloğa geç
            return INSTANCE ?: synchronized(this) {
                // Aynı anda birden fazla thread'in buraya girip birden fazla veritabanı açmasını engeller
                val instance = Room.databaseBuilder(
                    context.applicationContext, // Bellek sızıntısını önlemek için uygulama context'i kullanılır
                    PrayerDatabase::class.java,
                    "prayer_tracker_db" // Cihazın diskinde saklanacak veritabanı dosyasının adı
                )
                    // Versiyon yükseltmelerinde (Migration) hata çıkarsa eski verileri silip tabloyu yeniden kurar
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance // Oluşturulan nesneyi geri döndür
            }
        }
    }
}
