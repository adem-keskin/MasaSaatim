package com.masasaatim.domain.model

/**
 * UI ve Domain katmanında kullanılacak saf ezan vakitleri veri modeli.
 */
data class PrayerTime(
    val date: String,       // Örn: "04.06.2026"
    val imsak: String,      // Örn: "03:42"
    val gunes: String,      // Örn: "05:18"
    val ogle: String,       // Örn: "13:12"
    val ikindi: String,     // Örn: "17:05"
    val aksam: String,      // Örn: "20:54"
    val yatsi: String       // Örn: "22:21"
)