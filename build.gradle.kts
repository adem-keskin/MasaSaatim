plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false

    // TIRE YERİNE NOKTA KULLANARAK KOTLIN DSL HATASINI ÇÖZÜYORUZ:
    alias(libs.plugins.google.devtools.ksp) apply false
}
