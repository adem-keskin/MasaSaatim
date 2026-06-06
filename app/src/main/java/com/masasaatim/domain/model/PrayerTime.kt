package com.masasaatim.domain.model

/**
 * PrayerTime Veri Sınıfı (Data Class): Kullanıcı arayüzünde (UI / Tasarım) ve
 * iş mantığı (Domain) katmanında doğrudan kullanılacak olan saf namaz vakitleri veri modelidir.
 *
 * Bu sınıf hiçbir harici kütüphaneye (@Entity, @SerializedName vb.) bağımlı değildir.
 * Projenin gelecekte başka bir platforma taşınmasını veya test edilmesini (Unit Test) inanılmaz kolaylaştırır.
 */
data class PrayerTime(
    // Günün benzersiz tarihi. Ekranda başlık olarak veya sorgulama anahtarı olarak kullanılır. (Örn: "2026-06-05")
    val date: String,

    // Aşağıdaki tüm vakitler, masa saati ekranında canlı gösterilmek veya geri sayım
    // sayaçlarında (Countdown) kullanılmak üzere saf metin (String) olarak 24 saat formatında tutulur.
    val imsak: String,      // Sabah namazı vakti başlangıcı / Sahur bitişi (Örn: "03:42")
    val gunes: String,      // Güneş doğuş saati / Kerahet vakti başlangıcı (Örn: "05:18")
    val ogle: String,       // Öğle namazı vakti başlangıcı (Örn: "13:12")
    val ikindi: String,     // İkindi namazı vakti başlangıcı (Örn: "17:05")
    val aksam: String,      // Akşam namazı vakti başlangıcı / İftar vakti (Örn: "20:54")
    val yatsi: String       // Yatsı namazı vakti başlangıcı (Örn: "22:21")
)
