plugins {
    // Eklentiler (Plugins): Projenin Android Uygulaması, Kotlin ve KSP (Room derleyicisi) desteklemesini sağlar.
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.devtools.ksp) // Room tablolarını arka planda Java/Kotlin koduna dönüştüren hızlı derleyici
    alias(libs.plugins.compose.compiler) // Yeni nesil Compose Compiler eklentisi (Kotlin 2.0+)
}

android {
    namespace = "com.masasaatim"
    compileSdk = 35 // Uygulamanın derlenirken kullandığı en yüksek Android SDK sürümü (Android 15)

    defaultConfig {
        applicationId = "com.masasaatim" // Google Play Store'daki benzersiz uygulama kimliğiniz
        minSdk = 26
        targetSdk = 35 // Uygulamanın tam uyumlu ve optimize test edildiği hedef sürüm (Android 15)
        versionCode = 1 // Google Play'in güncellemeleri anlaması için dahili sürüm numarası
        versionName = "1.0.0" // Kullanıcıların gördüğü vitrin sürüm adı

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true // Vektör (SVG) ikonların eski cihazlarda da sorunsuz çizilmesini sağlar
        }
    }

    buildTypes {
        // Canlıya çıkış (Release) ayarları: Uygulama mağazaya yüklenirken yapılacak işlemler
        release {
            isMinifyEnabled = true // Proguard/R8 kod karartmasını açar: Kodun çalınmasını zorlaştırır, boyutu küçültür
            isShrinkResources = true // Kullanılmayan tüm gereksiz resim ve kaynakları temizler: APK boyutunu düşürür
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Java derleme uyumluluğu: Modern Android standartı olan Java 17 sürümüne ayarlandı
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17" // Kotlin kodlarının Java 17 sanal makinesine (JVM) uygun derlenmesini sağlar
        // 🌟 Uyumsuz metadata sürüm hatalarını pas geçer (play-services-location kilitlenmesini çözer)
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }
    buildFeatures {
        compose = true // Jetpack Compose (Modern UI) özelliğini projede aktif eder
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}" // Çift kütüphane lisans çakışmalarını önleyen paketleme filtresi
        }

        jniLibs {
            // Native kütüphanelerin sıkıştırılmadan 16 KB sınırlarında kalmasını sağlar (Android 15+ Zorunluluğu)
            useLegacyPackaging = false
        }
    }
}

// 🌟 SÜRÜM GÜVENLİK KİLİDİ: Kontrolden çıkan kütüphaneleri API 35 sınırında kalmaya zorlar
configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.15.0")
        force("androidx.core:core-ktx:1.15.0")
        force("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
        force("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
        force("androidx.activity:activity:1.9.3")
        force("androidx.activity:activity-ktx:1.9.3")
        force("androidx.activity:activity-compose:1.9.3")
    }
}

dependencies {
    // --- TEMEL ANDROID VE YAŞAM DÖNGÜSÜ KÜTÜPHANELERİ ---
    implementation(libs.androidx.core.ktx) // Kotlin için temel Android uzantıları
    implementation(libs.androidx.lifecycle.runtime.ktx) // Aktivite yaşam döngüsü coroutine destekleri
    implementation(libs.androidx.lifecycle.viewmodel.compose) // ViewModel'lerin Jetpack Compose içinde kullanımı için
    implementation(libs.androidx.activity.compose) // Compose arayüzünü başlatan ComponentActivity desteği

    // --- JETPACK COMPOSE UI KATMANI ---
    implementation(platform(libs.androidx.compose.bom)) // Tüm Compose kütüphanelerinin sürümlerini senkronize eden ana paket (BOM)
    implementation(libs.androidx.compose.ui) // Ekran çizim ve tasarım motoru
    implementation(libs.androidx.compose.ui.graphics) // Gelişmiş renk, tuval ve çizim araçları
    implementation(libs.androidx.compose.ui.tooling.preview) // Android Studio'da anlık tasarım önizleme aracı
    implementation(libs.androidx.compose.material3) // Google Material 3 tasarım bileşenleri (Button, Switch, Text vb.)
    implementation("androidx.compose.material:material-icons-core") // Standart Material İkonları kütüphanesi
    implementation("androidx.compose.material:material-icons-extended") // Genişletilmiş İkon Paketi (LocationOn, PlayArrow vb.)

    // --- YEREL VERİTABANI (ROOM) ---
    implementation(libs.androidx.room.runtime) // Room veritabanının çekirdek motoru
    implementation(libs.androidx.room.ktx) // Room sorgularının Coroutine ve Flow (Canlı Akış) ile çalışmasını sağlar
    ksp(libs.androidx.room.compiler) // Derleme aşamasında veritabanı tablolarını (Entities) üreten KSP işlemcisi

    // --- ARKA PLAN VE MULTİMEDYA ---
    implementation(libs.androidx.work.runtime.ktx) // Zamanlanmış arka plan görevleri için WorkManager
    implementation(libs.androidx.media3.exoplayer) // 🌟 DÜZELTİLDİ: Manüel metin satırı silindi, sürüm kataloğuna bağlandı

    // --- İNTERNET VE API BAĞLANTI KATMANI (RETROFIT) ---
    implementation(libs.play.services.location) // Donanımsal GPS konum takibi için Google Play Servisleri
    implementation(libs.retrofit) // Aladhan API'sine HTTP istekleri atan ana network kütüphanesi
    implementation(libs.converter.gson) // API'den gelen JSON verilerini otomatik nesnelere dönüştürücü

    // --- SÜRÜM KATALOĞUNDAN BESLENEN STABİL ANDROID 15 BİLEŞENLERİ ---
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.ui.text.googlefonts)
    implementation("androidx.graphics:graphics-path:1.1.0")
    implementation("androidx.graphics:graphics-shapes:1.1.0")
}
