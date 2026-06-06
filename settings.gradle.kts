// --- EKLENTİ YÖNETİMİ (Plugin Management) ---
// Gradle derleme eklentilerinin (Android ve Kotlin eklentileri gibi) hangi güvenli kaynaklardan indirileceğini belirler.
pluginManagement {
    repositories {
        // Google Resmi Deposu: Android'e özel geliştirilen tüm resmi derleme araçlarının merkezidir.
        google {
            // GÜVENLİK FİLTRESİ (Regex): Sadece doğrulanmış Google ve AndroidX paketlerinin bu depodan indirilmesine izin verir.
            // "Dependency Confusion" (Bağımlılık Karışıklığı) adı verilen siber saldırıları engelleyerek projenin güvenliğini sağlar.
            content {
                includeGroupByRegex("com\\.android.*") // Sadece resmi Android çekirdek eklentileri
                includeGroupByRegex("com\\.google.*")  // Sadece resmi Google eklentileri
                includeGroupByRegex("androidx.*")     // Sadece modern AndroidX eklentileri
            }
        }
        // Maven Central: Java ve Kotlin dünyasındaki açık kaynaklı kütüphanelerin küresel ve en büyük ana merkezidir.
        mavenCentral()

        // Gradle Plugin Portal: Gradle topluluğu tarafından geliştirilen genel eklentilerin barındığı resmi kapıdır.
        gradlePluginPortal()
    }
}

// --- KÜRESEL DERLEME EKLENTİLERİ ---
// Tüm Gradle sisteminin altyapısını ve geliştirici bilgisayarındaki uyumluluğu yöneten küresel eklentiler.
plugins {
    // Foojay Resolver: Bilgisayarınızda uyumlu bir Java JDK yüklü değilse, projenin derlenebilmesi için
    // gerekli olan doğru Java sürümünü arka planda otomatik bulur ve indirir (Java Toolchain uyumluluğu sağlar).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// --- BAĞIMLILIK ÇÖZÜMLEME YÖNETİMİ (Dependency Resolution Management) ---
// Uygulama içinde kullandığımız tüm harici kütüphanelerin (Retrofit, Room, Media3 vb.) indirileceği marketleri merkezi olarak yönetir.
dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS: Temiz mimari kuralıdır. Alt modüllerin (app modülü gibi) kendi içlerindeki build.gradle
    // dosyalarında bağımsız/kontrolsüz kütüphane deposu tanımlamasını yasaklar. Tüm kaynaklar tek merkezden (buradan) deklare edilmelidir.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    // MasaSaatim uygulamasının tüm kütüphanelerini indirebileceği merkezi ve izinli marketler tanımlanıyor:
    repositories {
        google()       // Android bileşenleri için (Jetpack Compose, Material 3, Room, WorkManager)
        mavenCentral() // Açık kaynaklı harici kütüphaneler için (Retrofit internet istemcisi, GSON dönüştürücü)
    }
}

// --- PROJE YAPISI VE İSİMLENDİRME ---
// Projenin Android Studio üzerinde görünecek resmi ve küresel adını belirler.
rootProject.name = "MasaSaatim"

// Uygulamanın asıl kaynak kodlarının, tasarımlarının ve servislerinin yer aldığı ana dizini (app klasörünü) projeye dahil eder.
include(":app")
