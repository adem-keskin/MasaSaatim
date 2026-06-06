package com.masasaatim.data.local.entity

// Room kütüphanesinin veri tabanı tablosu oluşturmak için kullandığı bileşenler içe aktarılıyor.
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PrayerEntity Sınıfı: Bu sınıf, veri tabanındaki bir satırlık veriyi (kaydı) temsil eder.
 * @Entity: Room kütüphanesine bu sınıfın bir veri tabanı tablosu olduğunu bildirir.
 * tableName = "prayer_times": SQLite içinde oluşacak tablonun adını "prayer_times" olarak belirler.
 */
@Entity(tableName = "prayer_times")
data class PrayerEntity(

    /**
     * @PrimaryKey: Bu alanın tablodaki "Birincil Anahtar" (Benzersiz Kimlik) olduğunu belirtir.
     * Veri tabanında aynı tarihe ait iki farklı kayıt olamaz.
     * Eğer aynı tarihle yeni bir veri eklenirse, eski verinin üzerine yazılır (Upsert/Replace mantığı).
     *
     * val date: String -> Örn: "2026-06-05" (yyyy-MM-dd) formatında günün tarihidir.
     * Bu format, veri tabanında tarih sıralaması (A-Z veya Z-A) yapmayı son derece kolaylaştırır.
     */
    @PrimaryKey
    val date: String,

    // Günün 6 temel vakti, 24 saatlik zaman formatında metin (String) olarak saklanır (Örn: "03:45", "21:15")
    val imsak: String,   // Sabah namazı / İmsak vakti başlangıcı
    val gunes: String,   // Güneşin doğuş saati
    val ogle: String,    // Öğle namazı vakti
    val ikindi: String,  // İkindi namazı vakti
    val aksam: String,   // Akşam namazı / İftar vakti
    val yatsi: String    // Yatsı namazı vakti
)
