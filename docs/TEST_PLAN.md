# Test planı

Bu rapor 14 Ekim 2026 tarihinde Nothing A063 (Android 15) ve Samsung SM-M127F (Android 11) ile güncellenmiştir.

| Test | Durum | Beklenen sonuç |
|---|---|---|
| Flutter unit/widget | PASS | `flutter test`: 2 test geçti |
| Flutter analiz | PASS | `flutter analyze`: sorun bulunmadı |
| Kotlin packet/arbitration | PASS | `:app:testDebugUnitTest`: binary round-trip, bozuk paket ve tek initiator testleri geçti |
| Android debug APK | PASS | `flutter build apk --debug` tamamlandı |
| 1 — İki telefon keşfi | PASS | İki cihaz birbirini Nearby üzerinden buldu |
| 2 — Otomatik bağlantı | PASS | Tek mantıksal oturum kuruldu; simetrik istek çökmesi görülmedi |
| 3 — İnternetsiz | NOT TESTED | Mobil veri/internet kapalıyken keşif ve ses sürer |
| 4 — Telefon sesi | PARTIAL | Çift yönlü anlaşılır ses doğrulandı; STREAM sonrası Samsung gecikme/sonlandırma optimizasyonunun son kullanıcı doğrulaması bekleniyor |
| 5 — İki Bluetooth interkom | NOT TESTED | HFP mikrofon ve çıkış iki yönde çalışır |
| 6 — Ekran kapalı | NOT TESTED | Kurulu görüşme foreground service ile sürer; kilitliyken ilk başlangıç ayrıca ölçülür |
| 7 — Kopma/yeniden bağlantı | PARTIAL | Kaynak kapanışı ve yeniden keşif loglarda doğrulandı; tekrarlı uzun süre testi bekleniyor |
| 8 — Hata durumları | PASS | Wi-Fi izni, eski Android konum izni ve EventChannel thread çökmeleri giderildi |

Her fiziksel koşuda iki telefonun modelini, Android sürümünü, interkom modelini, kullanılan audio route'u, codec bulunabilirliğini, ilk bağlantı süresini, ortalama paket kaybını ve PASS/FAIL sonucunu kaydedin.

Not: Üst düzey Gradle `testDebugUnitTest`, RideLink testlerinden sonra `shared_preferences_android` eklentisinin Robolectric testinde Java 21 gereksinimi nedeniyle başarısız oldu. RideLink'e ait izole `:app:testDebugUnitTest` başarılıdır; ortam JDK'sı Java 17'dir.
