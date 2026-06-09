package com.masasaatim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room veritabanında bir tabloyu temsil eden Entity veri sınıfı.
 * Bu sınıf, cihazın yerel hafızasında "prayer_times" adında bir SQL tablosu oluşturur.
 */
@Entity(tableName = "prayer_times")
data class PrayerEntity(

    // 'date' (Tarih) alanı bu tablonun Birincil Anahtarıdır (Primary Key).
    // Yani veritabanında aynı tarihe ait sadece tek bir kayıt bulunabilir (Örn: "2026-06-08").
    @PrimaryKey
    val date: String,

    // Ezan vakitlerinin saat değerlerini (Örn: "03:45", "13:20") metin (String) olarak tutan kolonlar:
    val imsak: String,  // İmsak vakti
    val gunes: String,  // Güneş doğuş vakti
    val ogle: String,   // Öğle ezanı vakti
    val ikindi: String, // İkindi ezanı vakti
    val aksam: String,  // Akşam ezanı vakti
    val yatsi: String   // Yatsı ezanı vakti
)
