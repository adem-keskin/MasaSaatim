package com.masasaatim.domain.model

/**
 * Clean Architecture mimarisinin en kalbinde (Domain katmanında) yer alan veri modeli.
 * Bu sınıf, uygulamanın arayüzünde (UI) kullanıcıya doğrudan gösterilecek olan temiz ezan vakti verilerini taşır.
 * Hiçbir kütüphaneye bağımlılığı olmayan saf bir Kotlin nesnesidir.
 */
data class PrayerTime(
    val date: String,   // Standart formatta temiz tarih bilgisi (Örn: "2026-07-13")
    val imsak: String,  // Temizlenmiş imsak vakti (Örn: "03:45")
    val gunes: String,  // Temizlenmiş güneş doğuş vakti (Örn: "05:20")
    val ogle: String,   // Temizlenmiş öğle ezanı vakti (Örn: "13:12")
    val ikindi: String, // Temizlenmiş ikindi ezanı vakti (Örn: "17:01")
    val aksam: String,  // Temizlenmiş akşam ezanı vakti (Örn: "20:45")
    val yatsi: String   // Temizlenmiş yatsı ezanı vakti (Örn: "22:15")
) {
    /**
     * 🌟 YENİ: Vakitleri isimleriyle birlikte eşleştirilmiş bir liste (Map) olarak döner.
     * Bu fonksiyon sayesinde ViewModel içinde sıradaki vakti bulmak için tek tek 'if-else'
     * yazmak yerine, tüm vakitleri tek bir döngüde kolayca tarayabilirsiniz.
     */
    fun asOrderedMap(): List<Pair<String, String>> {
        return listOf(
            "İmsak" to imsak,
            "Güneş" to gunes,
            "Öğle" to ogle,
            "İkindi" to ikindi,
            "Akşam" to aksam,
            "Yatsı" to yatsi
        )
    }
}
