pluginManagement {
    // Proje genelinde kullanılacak Gradle eklentilerinin (Plugins) hangi internet depolarından indirileceğini belirler.
    repositories {
        google {
            // Güvenlik ve Hız Optimizasyonu: Sadece Android, Google ve AndroidX kütüphane eklentilerinin
            // Google depolarında aranmasını sağlayarak gereksiz indirme isteklerini ve çakışmaları önler.
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral() // Dünyanın en büyük genel kütüphane ve eklenti deposu
        gradlePluginPortal() // Gradle topluluğu tarafından geliştirilen resmi eklenti havuzu
    }
}

plugins {
    // Foojay Toolchain Resolver: Android Studio'nun projeyi derlemek için ihtiyaç duyduğu
    // Java Sürümünü (Örn: JDK 17) internetten otomatik olarak bulup indirmesini sağlayan modern eklenti.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS -> Projedeki tüm modüllerin kütüphane depolarını tek bir merkezden (aşağıdaki bloktan)
    // yönetmeye zorlar. Alt modüller kendi içlerinde kafalarına göre depo tanımlarsa derleme aşamasında hata (Fail) fırlatır.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    // Uygulamanın içerisindeki Retrofit, Room, ExoPlayer gibi tüm kütüphanelerin indirileceği güvenli depolar:
    repositories {
        google() // Google'ın resmi Android kütüphaneleri deposu
        mavenCentral() // Üçüncü parti açık kaynak kodlu kütüphane deposu
    }
}

// Projenin Android Studio ve işletim sistemindeki ana (kök) adı
rootProject.name = "MasaSaatim"

// Projeye dahil edilen modülleri belirtir. ':app' sizin tüm arayüz, servis ve veri kodlarınızı içeren ana modülünüzdür.
include(":app")
