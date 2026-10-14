# Değişiklik günlüğü

## Yayınlanmamış — 14 Ekim 2026

### Düzeltmeler

- Android 12+ cihazlarda Nearby keşfini `8032 / MISSING_PERMISSION_ACCESS_WIFI_STATE` ile durduran Wi-Fi izin kapsamı düzeltildi.
- Android 11 ve altı için gerekli çalışma zamanı konum izni Flutter başlangıç akışına eklendi.
- Native olayların arka plan thread'inden Flutter `EventChannel`'ına gönderilmesiyle oluşan `@UiThread` çökmesi giderildi.
- Opus decoder için zorunlu `csd-0`, `csd-1` ve `csd-2` başlangıç verileri eklendi.
- Ses hattı Opus'un doğal 48 kHz saatine geçirildi; bozuk/anlaşılmaz PCM oynatma giderildi.
- Telefonla test modunda sesin ahize yerine dış hoparlöre yönlendirilmesi sağlandı.
- Codec ve AudioTrack yaşam döngüsü yarışları oturum generation kontrolü ve eşzamanlı kapatma ile giderildi.

### Gecikme ve yaşam döngüsü

- Ses taşımacılığı her kare için ayrı Nearby `BYTES` göndermek yerine sürekli `STREAM` kullanacak şekilde değiştirildi.
- Yavaş alıcıların aktarım kuyruğu büyütmesini önlemek için üç karelik, en güncel sesi koruyan sınırlı gönderim kuyruğu eklendi.
- Jitter ve AudioTrack tamponları düşük gecikme için küçültüldü.
- STREAM yazımı ayrı executor'a taşınarak “Görüşmeyi Sonlandır” ve “Sürüş Modunu Durdur” işlemlerinin taşıma yazımını beklemesi engellendi.
- Kapanışta endpoint önce geçersiz kılınıyor; eski yakalama işlerinin kapanmış bağlantıda yeni STREAM oluşturması engellendi.
- Flutter durumu durdurma/sonlandırma dokunuşunda iyimser olarak anında güncelleniyor; native kaynak temizliği ardından tamamlanıyor.

### Doğrulama

- `flutter analyze`: PASS.
- `flutter test`: 2/2 PASS.
- Debug APK: PASS.
- Nothing A063 (Android 15) + Samsung SM-M127F (Android 11): keşif, otomatik eşleşme, çift yönlü STREAM ve 48 kHz kayıt/oynatma doğrulandı.
