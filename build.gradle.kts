plugins {
    // alias(libs.versions.toml): Versiyon kataloğundan (libs.versions.toml) ilgili eklentiyi sürümüyle bulur.
    // apply false: Bu eklentiyi kök (ana) proje seviyesinde çalıştırma, sadece alt modüllerin (app modülü gibi)
    //              kullanabilmesi için merkezi olarak kaydet ve hazırda beklet anlamına gelir.

    // 1. Eklenti: Android uygulamasının derleme kurallarını belirleyen ana uygulama eklentisini merkezi olarak kaydeder.
    alias(libs.plugins.android.application) apply false

    // 2. Eklenti: Kotlin programlama dilinin Android Studio ve Android SDK ile uyumlu çalışmasını sağlayan eklentiyi kaydeder.
    alias(libs.plugins.jetbrains.kotlin.android) apply false

    // 3. Eklenti: Room veri tabanı anotasyonlarını işleyen, eski kapt teknolojisine göre 2 kat daha hızlı derleme sunan
    //            ve tire yerine nokta kullanımıyla güncellenmiş KSP (Kotlin Symbol Processing) eklentisini merkezi olarak kaydeder.
    alias(libs.plugins.google.devtools.ksp) apply false
}
