package com.masasaatim.domain.model

/**
 * Kullanıcının ezan vakitlerini görmek istediği konumu temsil eden Domain veri modeli.
 * Hem manuel seçilen şehirleri hem de GPS ile tespit edilen konumları ortak bir yapıda tutar.
 */
data class SavedLocation(
    val cityName: String,    // Konumun ekranda gösterilecek adı (Örn: "Ankara", "Berlin" veya "Mevcut Konum")
    val latitude: Double,    // Harita üzerindeki Enlem (Latitude) bilgisi (API ve hesaplamalar için gerekli)
    val longitude: Double,   // Harita üzerindeki Boylam (Longitude) bilgisi (API ve hesaplamalar için gerekli)

    // Konumun kaydetme türü:
    // 'true' -> Cihazın GPS/Konum servislerinden otomatik alındı.
    // 'false' -> Kullanıcı tarafından listeden veya arama kısmından el ile seçildi.
    val isAutomatic: Boolean
)
