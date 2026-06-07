package com.masasaatim.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masasaatim.data.local.entity.PrayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {

    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    fun getPrayerTimeByDate(date: String): Flow<PrayerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(prayerTimes: List<PrayerEntity>)
}
