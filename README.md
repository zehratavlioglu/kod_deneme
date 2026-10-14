# RideLink

RideLink, iki Android telefonun Google Nearby Connections üzerinden internetsiz olarak birbirini keşfetmesi, bağlanması ve çift yönlü konuşma sesi taşıması için geliştirilmiş bir Flutter/Kotlin PoC uygulamasıdır.

## Durum

- Flutter Material 3 arayüzü ve Riverpod durum yönetimi: uygulandı.
- Native Bluetooth iletişim cihazı tespiti ve ses rotası: uygulandı; telefon hoparlörü test rotası fiziksel cihazlarda **PASS**, gerçek Bluetooth interkom **NOT TESTED**.
- Nearby P2P_POINT_TO_POINT keşif/advertising/bağlantı: Nothing A063 (Android 15) ve Samsung SM-M127F (Android 11) ile **PASS**.
- Native AudioRecord/AudioTrack ve MediaCodec Opus hattı: iki telefonda çift yönlü anlaşılır ses **PASS**; Samsung gecikme optimizasyonunun son kullanıcı doğrulaması sürüyor.
- Foreground service (`connectedDevice|microphone`): uygulandı, ekran kapalı davranışı **NOT TESTED**.
- Debug APK derlemesi: **PASS** — `build/app/outputs/flutter-apk/app-debug.apk`.

## Çalıştırma

### Gereksinimler

- Flutter 3.44 veya uyumlu güncel stable sürüm
- Dart 3.12+
- JDK 17
- Android SDK 36; minimum cihaz sürümü API 26
- Google Play services bulunan iki gerçek Android telefon

### Yerel geliştirme

```bash
flutter pub get
flutter analyze
flutter test
flutter run
```

Android/Kotlin testleri ve APK üretimi:

```bash
cd android
./gradlew :app:testDebugUnitTest
cd ..
flutter build apk --debug
```

Uygulamada izinleri verin. İlk testte “Telefon sesiyle test” seçeneğini kullanabilirsiniz. Her iki cihazda “Otomatik eşleşme”yi açıkça etkinleştirip ardından sürüş modunu başlatın.

Fiziksel test adımları için [docs/TEST_PLAN.md](docs/TEST_PLAN.md), bilinen riskler için [docs/KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md) dosyasına bakın.

## Güvenlik

İlk karşılaşmada otomatik kabul, Nearby doğrulama rakamlarını olay günlüğünde gösterse de karşıdaki kişinin kimliğini kanıtlamaz. Üretim sürümünde güvenilir eş/QR veya manuel rakam doğrulama akışı zorunlu hale getirilmelidir.

Bu depo özel olsa bile Android keystore dosyaları, `key.properties`, `.env` dosyaları, Google servis yapılandırmaları ve dışa aktarılan tanılama kayıtları Git'e eklenmez. Hassas değerleri yalnızca yerel veya CI secret deposunda saklayın.

## Belgeler

- [Mimari](docs/ARCHITECTURE.md)
- [Android izinleri](docs/ANDROID_PERMISSIONS.md)
- [Nearby protokolü](docs/NEARBY_PROTOCOL.md)
- [Ses hattı](docs/AUDIO_PIPELINE.md)
- [Test planı ve güncel sonuçlar](docs/TEST_PLAN.md)
- [Bilinen sınırlamalar](docs/KNOWN_LIMITATIONS.md)
- [Play Store kontrol listesi](docs/PLAY_STORE_CHECKLIST.md)
- [Değişiklik günlüğü](CHANGELOG.md)
