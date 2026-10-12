# Test planı

Bu rapor 11 Ekim 2026 tarihinde cihaz bağlı olmadan hazırlanmıştır. Fiziksel maddeler varsayılan olarak **NOT TESTED** durumundadır.

| Test | Durum | Beklenen sonuç |
|---|---|---|
| Flutter unit/widget | PASS | `flutter test`: 2 test geçti |
| Flutter analiz | PASS | `flutter analyze`: sorun bulunmadı |
| Kotlin packet/arbitration | PASS | `:app:testDebugUnitTest`: binary round-trip, bozuk paket ve tek initiator testleri geçti |
| Android debug APK | PASS | `flutter build apk --debug` tamamlandı |
| 1 — İki telefon keşfi | NOT TESTED | Her cihaz diğer geçici RideLink kimliğini listeler |
| 2 — Otomatik bağlantı | NOT TESTED | Yalnız tek mantıksal oturum oluşur |
| 3 — İnternetsiz | NOT TESTED | Mobil veri/internet kapalıyken keşif ve ses sürer |
| 4 — Telefon sesi | NOT TESTED | İki yönde anlaşılır, düşük gecikmeli konuşma |
| 5 — İki Bluetooth interkom | NOT TESTED | HFP mikrofon ve çıkış iki yönde çalışır |
| 6 — Ekran kapalı | NOT TESTED | Kurulu görüşme foreground service ile sürer; kilitliyken ilk başlangıç ayrıca ölçülür |
| 7 — Kopma/yeniden bağlantı | NOT TESTED | Kaynaklar kapanır, keşif yeniden başlar, çökme olmaz |
| 8 — Hata durumları | NOT TESTED | Bluetooth/izin/rota/mikrofon hataları Türkçe gösterilir |

Her fiziksel koşuda iki telefonun modelini, Android sürümünü, interkom modelini, kullanılan audio route'u, codec bulunabilirliğini, ilk bağlantı süresini, ortalama paket kaybını ve PASS/FAIL sonucunu kaydedin.

Not: Üst düzey Gradle `testDebugUnitTest`, RideLink testlerinden sonra `shared_preferences_android` eklentisinin Robolectric testinde Java 21 gereksinimi nedeniyle başarısız oldu. RideLink'e ait izole `:app:testDebugUnitTest` başarılıdır; ortam JDK'sı Java 17'dir.
