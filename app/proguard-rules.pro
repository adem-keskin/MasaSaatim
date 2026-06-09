# 1. ÇEKİRDEK KOD ÖZNİTELİKLERİNİ KORUMA
# Yansıtma (Reflection) ve JSON dönüştürme (GSON) işlemlerinin arka planda sorunsuz çalışabilmesi için
# Kotlin anotasyonlarını (*Annotation*), jenerik tür imzalarını (Signature), iç sınıfları (InnerClasses)
# ve metot çevreleme bilgilerini (EnclosingMethod) ProGuard temizliğinden muaf tutar.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 2. API VERİ MODELLERİNİ (DTO) BÜTÜNSEL KORUMA
# 'com.masasaatim.data.model' paketi altındaki tüm sınıfları ve içeriklerini tamamen korur.
# GSON kütüphanesinin internetten gelen JSON verilerini bu sınıflara hatasız eşlemesini (parsing) sağlar.
-keep class com.masasaatim.data.model.** { *; }

# 3. RETROFIT INTERFACE KATMANINI KORUMA
# 'com.masasaatim.core.network' paketi altındaki 'PrayerApiService' gibi ağ arayüzlerini korur.
# Retrofit'in çalışma zamanında (Runtime) dinamik proxy sınıfları üretebilmesi için bu isimlerin karartılmaması şarttır.
-keep class com.masasaatim.core.network.** { *; }

# 4. GSON ALANLARINI (FIELDS) KORUMA
# Veri modellerinin içerisindeki değişken adlarının (Örn: fajr, dhuhr) karartılmasını önler.
# Eğer bu isimler 'a', 'b' gibi harflere dönüştürülseydi, GSON @SerializedName anotasyonlarını eşleştiremezdi.
-keepclassmembers class com.masasaatim.data.model.** { <fields>; }

# 5. YEREL VERİTABANI (ROOM) ENTITY SINIFLARINI KORUMA
# 'com.masasaatim.data.local.entity' altındaki 'PrayerEntity' gibi Room tablo sınıflarını korur.
# Room kütüphanesinin SQL tabloları ile Kotlin nesneleri arasında veri transferi yaparken hata vermesini engeller.
-keep class com.masasaatim.data.local.entity.** { *; }
