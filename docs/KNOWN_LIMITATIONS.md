# Bilinen sınırlamalar

- Bu çalışma ortamına fiziksel Android telefon bağlı değildir; Nearby, Bluetooth HFP, gerçek mikrofon, ekran kapalı davranış ve çift yönlü ses doğrulanmamıştır.
- MediaCodec Opus encoder donanıma/üreticiye bağlıdır. Encoder olmayan cihazda uygulama bilinçli olarak başlamaz; JNI libopus fallback henüz yoktur.
- Nearby, Google Play services gerektirir. Radyo taşıyıcısını uygulama kesin olarak seçemez; internetsiz çalışması hedeflenir fakat Bluetooth/Wi‑Fi radyoları gerekebilir.
- Sessiz ilk eşleşme insan kimliğini doğrulamaz. Nearby rakamları kaydedilir ancak otomatik akışta kullanıcı karşılaştırması yoktur.
- Yeniden keşif anında başlar; exponential backoff ve bağlantı timeout watchdog'u sonraki stabilizasyon işidir.
- Telefon araması/audio focus kaybı için tam `TelecomManager` entegrasyonu henüz yoktur.
- Android 17 `ACCESS_LOCAL_NETWORK` runtime akışı kullanılan Flutter izin paketinde ayrıca doğrulanmalıdır.
- `permission_handler` 13.0.2, mevcut SDK yöneticisinin `android-37.0` paketi ile eklentinin beklediği `android-37` kimliği uyuşmadığı için derlenemedi; derlenebilir 12.0.1 sürümüne sabitlendi.
- Flutter 3.44.6 kurulumu özel/unknown channel üzerindedir ve bazı Android SDK lisansları kabul edilmemiştir. Debug derleme başarılı olsa da CI/üretim ortamı resmî stable channel kullanmalıdır.
- Log dışa aktarma ve hassas veri redaksiyonlu dosya üretimi henüz uygulanmadı; UI son 100 olayı bellekte tutar.
