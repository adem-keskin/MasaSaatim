package com.masasaatim.core.di

import android.content.Context
import com.masasaatim.core.network.PrayerApiService
import com.masasaatim.data.local.PrayerDatabase
import com.masasaatim.data.repository.PrayerRepositoryImpl
import com.masasaatim.domain.repository.PrayerRepository
import com.masasaatim.domain.usecase.FetchPrayerTimesUseCase
import com.masasaatim.domain.usecase.GetPrayerTimeUseCase
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer(private val context: Context) {

    private val apiService: PrayerApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PrayerApiService::class.java)
    }

    private val database: PrayerDatabase by lazy {
        PrayerDatabase.getDatabase(context)
    }

    val prayerRepository: PrayerRepository by lazy {
        PrayerRepositoryImpl(database.prayerDao(), apiService)
    }

    val getPrayerTimeUseCase: GetPrayerTimeUseCase by lazy {
        GetPrayerTimeUseCase(prayerRepository)
    }

    //  val fetchPrayerTimesUseCase: FetchPrayerTimesUseCase by lazy {
    //  FetchPrayerTimesUseCase(prayerRepository)
    //}

    val fetchPrayerTimesUseCase = FetchPrayerTimesUseCase()
}
