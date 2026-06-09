plugins {
    // apply false -> Bu eklentilerin sürümlerini proje genelinde (merkezi olarak) kaydeder
    // ancak kök projede doğrudan çalıştırmaz. Alt modüllerin (app modülü gibi) bu eklentileri
    // kendi ihtiyaçlarına göre sürüm belirtmeden (alias ile) çağırıp kullanabilmesini sağlar.

    // Android Uygulama Eklentisi: Projenin bir Android uygulaması olarak derlenebilmesi için gerekli altyapıyı tanımlar.
    alias(libs.plugins.android.application) apply false

    // Kotlin Android Eklentisi: Android projesi içinde Kotlin dilinin kullanılabilmesini ve derlenmesini sağlar.
    alias(libs.plugins.jetbrains.kotlin.android) apply false

    // KSP (Kotlin Symbol Processing) Eklentisi: Room veritabanı gibi kütüphanelerin kod derleme aşamasında
    // arka planda çok daha hızlı ve performanslı çalışmasını sağlayan yeni nesil kod üretici motoru.
    alias(libs.plugins.google.devtools.ksp) apply false
}
