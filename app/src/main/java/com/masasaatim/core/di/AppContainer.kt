package com.masasaatim.core.di

// Uygulama içinde ihtiyaç duyulan sistem ve kütüphane bileşenleri içe aktarılıyor (import).
import android.content.Context
import com.masasaatim.core.network.PrayerApiService
import com.masasaatim.data.local.PrayerDatabase

import com.masasaatim.data.repository.PrayerRepositoryImpl
import com.masasaatim.domain.repository.PrayerRepository
import com.masasaatim.domain.usecase.FetchPrayerTimesUseCase
import com.masasaatim.domain.usecase.GetPrayerTimeUseCase
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.getValue

/**
 * AppContainer Sınıfı: Uygulama genelinde kullanılacak olan nesnelerin (API servisi, veri tabanı,
 * depolar ve iş mantığı sınıfları) tek bir merkezden oluşturulmasını ve yönetilmesini sağlar.
 * Harici bir kütüphane (Hilt vb.) kullanmadan Manuel Bağımlılık Enjeksiyonu (Manual DI) yapmanıza yarar.
 *
 * @param context Android sistem özelliklerine ve veri tabanına erişmek için gereken uygulama bağlamı.
 */
class AppContainer(private val context: Context) {

    /**
     * apiService: İnternetten ezan vakitlerini çekmek için kullanılan Retrofit arayüzüdür.
     * "by lazy" kullanılmıştır; yani bu nesne uygulama ilk açıldığında değil,
     * kod içinde ona ilk kez ihtiyaç duyulduğunda (belleği yormamak için) oluşturulur.
     */
    private val apiService: PrayerApiService by lazy {
        Retrofit.Builder()
            // İstek atılacak ana sunucu adresi belirleniyor (aladhan.com API entegrasyonu)
            .baseUrl("https://api.aladhan.com")
            // Gelen JSON formatındaki verileri Kotlin nesnelerine otomatik dönüştürmek için Gson ekleniyor
            .addConverterFactory(GsonConverterFactory.create())
            // Retrofit istemcisi inşa ediliyor
            .build()
            // Tanımladığımız PrayerApiService arayüzüne göre çalışan canlı bir API servis nesnesi üretiliyor
            .create(PrayerApiService::class.java)
    }

    /**
     * database: Cihazın yerel hafızasında namaz vakitlerini saklayacak olan Room Veri Tabanı nesnesidir.
     * "by lazy" ile ilk çağrılmaya kadar ertelenir ve Singleton yapısı sayesinde sadece bir kez üretilir.
     */
    private val database: PrayerDatabase by lazy {
        // Context bilgisi kullanılarak veri tabanı dosyası açılıyor veya yoksa oluşturuluyor
        PrayerDatabase.getDatabase(context)
    }

    /**
     * prayerRepository: Veri kaynağı yönetim merkezidir (Data Layer ile Domain Layer arasındaki köprü).
     * Domain katmanındaki soyut arayüzü (PrayerRepository) temel alır, ancak arka planda
     * gerçek işi yapan PrayerRepositoryImpl sınıfını çalıştırır.
     * Hem yerel veri tabanına erişmek için 'Dao' nesnesini hem de internete erişmek için 'apiService' nesnesini içine alır.
     */
    val prayerRepository: PrayerRepository by lazy {
        PrayerRepositoryImpl(database.prayerDao(), apiService)
    }

    /**
     * getPrayerTimeUseCase: Kullanıcı arayüzünün (UI) yerel veri tabanından veya bellekten
     * kayıtlı namaz vakitlerini "okuması / getirmesi" için tasarlanmış tek bir iş mantığı (Use Case) nesnesidir.
     * İşlemi gerçekleştirebilmek için yukarıda oluşturulan prayerRepository nesnesine ihtiyaç duyar.
     */
    val getPrayerTimeUseCase: GetPrayerTimeUseCase by lazy {
        GetPrayerTimeUseCase(prayerRepository)
    }

    /**
     * fetchPrayerTimesUseCase: İnternetteki API sunucusundan güncel namaz vakitlerini
     * "çekip (fetch) yerel veri tabanına kaydetme" operasyonunu yürüten iş mantığı (Use Case) nesnesidir.
     * Yine veri kaynaklarına erişmek adına prayerRepository nesnesini bağımlılık olarak alır.
     */
    val fetchPrayerTimesUseCase: FetchPrayerTimesUseCase by lazy {
        FetchPrayerTimesUseCase(prayerRepository)
    }
}
