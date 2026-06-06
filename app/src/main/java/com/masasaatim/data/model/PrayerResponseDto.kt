package com.masasaatim.data.model

// Google'ın veri eşleme kütüphanesi içe aktarılıyor.
import com.google.gson.annotations.SerializedName

/**
 * Ana Yanıt Modeli (Root): API'den dönen en dıştaki JSON objesini karşılar.
 * Sunucu tüm aylık takvimi "data" isimli bir JSON dizisi (Array) içinde gönderir.
 */
data class PrayerResponseDto(
    // JSON'daki "data" anahtarını alır ve içindeki her bir günü 'PrayerDayDto' listesine dönüştürür.
    @SerializedName("data") val dataList: List<PrayerDayDto>
)

/**
 * Günlük Bilgi Modeli: Listenin içindeki her bir günü temsil eder.
 * Her günün içerisinde hem namaz saatleri hem de o günün tarih bilgisi yer alır.
 */
data class PrayerDayDto(
    // JSON'daki "timings" objesini alır; İmsak, Güneş, Öğle vb. saatleri barındırır.
    @SerializedName("timings") val timings: TimingsDto,

    // JSON'daki "date" objesini alır; o günün takvimsel tarih detaylarını barındırır.
    @SerializedName("date") val dateInfo: DateInfoDto
)

/**
 * Vakit Saatleri Modeli: API'den gelen 24 saatlik formatta (Örn: "04:12", "19:45")
 * string türündeki namaz ve ezan vakitlerini saklar.
 */
data class TimingsDto(
    @SerializedName("Fajr") val fajr: String,       // İmsak / Sabah namazı vakti
    @SerializedName("Sunrise") val sunrise: String,   // Güneşin doğuş vakti (Kerahet vakti başlangıcı)
    @SerializedName("Dhuhr") val dhuhr: String,     // Öğle namazı vakti
    @SerializedName("Asr") val asr: String,         // İkindi namazı vakti
    @SerializedName("Maghrib") val maghrib: String, // Akşam namazı vakti (İftar vakti)
    @SerializedName("Isha") val isha: String        // Yatsı namazı vakti
)

/**
 * Tarih Kapsayıcı Model: JSON hiyerarşisinde miladi takvime ulaşabilmek için
 * aradaki köprü görevi gören "date" objesinin iç yapısını temsil eder.
 */
data class DateInfoDto(
    // JSON içindeki "gregorian" (Miladi Takvim) objesine odaklanır.
    @SerializedName("gregorian") val gregorian: GregorianDto
)

/**
 * Miladi Takvim Detay Modeli: Günün insan tarafından okunabilir net tarihini tutar.
 */
data class GregorianDto(
    // Sunucudan gelen "04-06-2026" (Gün-Ay-Yıl) formatındaki tarih metnini saklar.
    // Bu veri, veri tabanına (Room) kayıt yaparken birincil anahtar (PrimaryKey) olarak kullanılabilir.
    @SerializedName("date") val readableDate: String
)
