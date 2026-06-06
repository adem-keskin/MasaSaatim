package com.masasaatim.data.local

// Room kütüphanesi ve Android Context bileşenleri içe aktarılıyor (import).
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.masasaatim.data.local.dao.PrayerDao
import com.masasaatim.data.local.entity.PrayerEntity

/**
 * @Database: Bu sınıfın bir Room Veri Tabanı olduğunu sisteme bildirir.
 * entities = [PrayerEntity::class]: Bu veri tabanının içinde "prayer_times" (PrayerEntity) tablosunun barınacağını belirtir.
 * version = 1: Veri tabanının ilk sürümü (versiyonu) olduğunu ifade eder.
 * exportSchema = false: Veri tabanı şema geçmişini (geçiş kayıtlarını) dışarıya bir JSON dosyası olarak aktarmayı kapatır.
 */
@Database(entities = [PrayerEntity::class], version = 1, exportSchema = false)
abstract class PrayerDatabase : RoomDatabase() {

    /**
     * prayerDao: Veri tabanına sorgu atmak (Ekleme, Silme, Güncelleme, Listeleme) için
     * kullanılacak olan fonksiyonların bulunduğu arayüze (DAO) erişim köprüsüdür.
     */
    abstract fun prayerDao(): PrayerDao

    companion object {
        /**
         * @Volatile: Bu değişkenin değerinde yapılan herhangi bir değişikliğin, işlemcinin tüm
         * çekirdekleri (Thread'ler) tarafından anında görünür olmasını sağlar. Bellek üzerindeki
         * senkronizasyon hatalarını (okuma/yazma çakışmalarını) tamamen engeller.
         */
        @Volatile
        private var INSTANCE: PrayerDatabase? = null

        /**
         * getDatabase: Veri tabanı nesnesini güvenli bir şekilde başlatan veya kurulmuş olan bağlantıyı döndüren fonksiyondur.
         *
         * @param context Veri tabanı dosyasının cihaz hafızasında güvenle oluşturulabilmesi için gereken uygulama bağlamı.
         */
        fun getDatabase(context: Context): PrayerDatabase {
            // Eğer INSTANCE daha önce oluşturulmuşsa doğrudan onu döndür (Mevcut bağlantıyı koru)
            return INSTANCE ?: synchronized(this) {
                // synchronized(this): Aynı anda birden fazla iş parçacığının (Thread) bu bloğa girip
                // yanlışlıkla iki farklı veri tabanı nesnesi üretmesini (Race Condition) engelleyen kilit mekanizmasıdır.

                val instance = Room.databaseBuilder(
                    context.applicationContext, // Bellek sızıntısını önlemek için Activity yerine en üst düzey Application Context kullanılır
                    PrayerDatabase::class.java,
                    "prayer_tracker_db" // Cihazın diskinde oluşacak SQLite dosyasının adı
                )
                    // fallbackToDestructiveMigration: İleride yeni bir tablo ekleyip versiyonu 2 yaptığınızda,
                    // göç (Migration) senaryosu yazmadıysanız uygulamanın çökmesi yerine eski verileri temizleyip
                    // veri tabanını sıfırdan sorunsuz kurmasını sağlar (Geliştirme aşamasında hayat kurtarır).
                    .fallbackToDestructiveMigration()
                    .build()

                // Oluşturulan canlı veri tabanı bağlantısını değişkene ata
                INSTANCE = instance
                // Üretilen nesneyi geri döndür
                instance
            }
        }
    }
}
