package com.masasaatim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_times")
data class PrayerEntity(
    @PrimaryKey
    val date: String, // Her gün için tek bir kayıt olacak (GG.AA.YYYY)
    val imsak: String,
    val gunes: String,
    val ogle: String,
    val ikindi: String,
    val aksam: String,
    val yatsi: String
)
