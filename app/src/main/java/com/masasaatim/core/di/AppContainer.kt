package com.masasaatim.core.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.masasaatim.core.network.PrayerApiService
import com.masasaatim.data.local.PrayerDatabase
import com.masasaatim.data.repository.PrayerRepositoryImpl
import com.masasaatim.domain.repository.PrayerRepository
import com.masasaatim.domain.usecase.GetPrayerTimeUseCase
// 🌟 KRİTİK İMPORT: Farklı pakette yer alan MainViewModel'i buraya bağlıyoruz
import com.masasaatim.presentation.MainViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Uygulamanın genelinde kullanılacak olan nesnelerin (Bağımlılıkların)
 * yönetimini ve üretimini sağlayan Manuel Dependency Injection (DI) Konteyner sınıfı.
 */
class AppContainer(private val context: Context) {

    // 1. Ağ Servisi (API) Oluşturulması
    // 'by lazy' kullanılarak bu servise ilk kez ihtiyaç duyulana kadar nesne oluşturulmaz (Performans kazancı).
    private val apiService: PrayerApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com") // Ezan vakitleri API'sinin ana adresi
            .addConverterFactory(GsonConverterFactory.create()) // Gelen JSON verilerini Kotlin nesnelerine dönüştürücü
            .build()
            .create(PrayerApiService::class.java) // API arayüzünün somut örneğini oluşturur
    }

    // 2. Yerel Veritabanı (Room) Oluşturulması
    // Cihazın hafızasında verileri saklamak için kullanılan Room Veritabanı örneğini getirir.
    private val database: PrayerDatabase by lazy {
        PrayerDatabase.getDatabase(context) // Singleton desenine sahip veritabanını çağırır
    }

    // 3. Veri Deposu (Repository) Oluşturulması
    // API ve Yerel Veritabanını (Dao) parametre olarak alıp veri akışını yöneten yapıyı kurar.
    // Dışarıya soyut (interface) olan PrayerRepository tipinde sunulur, içeride somut (Impl) sınıf üretilir.
    val prayerRepository: PrayerRepository by lazy {
        PrayerRepositoryImpl(database.prayerDao(), apiService)
    }

    // 4. İş Mantığı Katmanı (UseCase) Oluşturulması
    // Sadece ezan vakitlerini getirme görevine odaklanmış olan iş mantığı sınıfı.
    // İhtiyacı olan veri deposunu (repository) parametre olarak alır.
    val getPrayerTimeUseCase: GetPrayerTimeUseCase by lazy {
        GetPrayerTimeUseCase(prayerRepository)
    }

    // 5. ViewModel Üretim Fabrikası (Factory)
    // Android sisteminin MainViewModel'i doğru parametrelerle (Application, UseCase, Repository)
    // oluşturabilmesi için gerekli olan özel fabrikayı (Factory) sağlar.
    fun provideFactory(application: Application): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    // MainViewModel başlatılırken gerekli tüm bağımlılıklar burada içeriye aktarılır (Inject edilir).
                    return MainViewModel(
                        application,
                        getPrayerTimeUseCase,
                        prayerRepository // Veri deposu doğrudan ViewModel'e teslim ediliyor.
                    ) as T
                }
                throw IllegalArgumentException("Bilinmeyen ViewModel Sınıfı: ${modelClass.name}")
            }
        }
    }
}
