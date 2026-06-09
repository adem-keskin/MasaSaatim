package com.masasaatim.data.model

import com.google.gson.annotations.SerializedName

/**
 * API'den dönen ana (kök) yanıt modelidir.
 * @SerializedName("data") -> JSON içerisindeki "data" isimli anahtarın (key)
 * karşılığını 'dataList' değişkenine eşitler. Bir aylık günlerin listesini tutar.
 */
data class PrayerResponseDto(
    @SerializedName("data") val dataList: List<PrayerDayDto>
)

/**
 * Belirli bir güne ait ezan vakitlerini ve tarih bilgilerini bir arada tutan model.
 */
data class PrayerDayDto(
    @SerializedName("timings") val timings: TimingsDto, // Saat bilgileri (İmsak, Öğle vb.)
    @SerializedName("date") val dateInfo: DateInfoDto    // Tarih bilgileri (Miladi takvim vb.)
)

/**
 * Gün içindeki vakitlerin saatlerini tutan model.
 * Aladhan API'sindeki İngilizce isimler (Fajr, Isha vb.), projenizin daha anlaşılır
 * olması için yerel değişken adlarına (fajr, isha vb.) haritalanır.
 */
data class TimingsDto(
    @SerializedName("Fajr") val fajr: String,       // İmsak / Sabah namazı vakti başlangıcı
    @SerializedName("Sunrise") val sunrise: String,   // Güneş'in doğuş vakti
    @SerializedName("Dhuhr") val dhuhr: String,     // Öğle ezanı vakti
    @SerializedName("Asr") val asr: String,         // İkindi ezanı vakti
    @SerializedName("Maghrib") val maghrib: String, // Akşam ezanı vakti
    @SerializedName("Isha") val isha: String        // Yatsı ezanı vakti
)

/**
 * Tarih bilgilerinin yer aldığı ara katman modeli.
 * API yanıtı içerisindeki "gregorian" (Miladi takvim) nesnesine erişimi sağlar.
 */
data class DateInfoDto(
    @SerializedName("gregorian") val gregorian: GregorianDto
)

/**
 * Miladi takvime göre okunabilir tarih formatını tutan nihai model.
 * @SerializedName("date") -> API'den gelen "08-06-2026" (GG-AA-YYYY) gibi metinsel tarih bilgisini alır.
 */
data class GregorianDto(
    @SerializedName("date") val readableDate: String
)
