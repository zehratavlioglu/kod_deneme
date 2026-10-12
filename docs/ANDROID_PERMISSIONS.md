# Android izinleri

- API 26–28: Nearby keşfi için `ACCESS_COARSE_LOCATION`.
- API 29–31: `ACCESS_FINE_LOCATION`.
- API 31+: `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`.
- API 33+: `NEARBY_WIFI_DEVICES` ve bildirim için `POST_NOTIFICATIONS`.
- API 37 hedeflendiğinde Nearby `WIFI_LAN` taşıyıcısı için `ACCESS_LOCAL_NETWORK` runtime izni ayrıca ele alınmalıdır. Manifest beyanı şimdiden mevcuttur; mevcut Flutter izin eklentisinde API 37 akışı doğrulanmamıştır.
- Ses için `RECORD_AUDIO` ve `MODIFY_AUDIO_SETTINGS`.
- Android 14+ için `FOREGROUND_SERVICE_CONNECTED_DEVICE` ve `FOREGROUND_SERVICE_MICROPHONE`.

Uygulama konum verisi üretmez veya saklamaz; bu nedenle Bluetooth ve Wi‑Fi yakındaki cihaz izinlerinde `neverForLocation` beyan edilir. Android 11 ve altındaki Nearby gereksinimi nedeniyle eski sürümlerde konum izni yine istenir.

Mikrofon foreground service'i yalnızca kullanıcı uygulama görünürken “Sürüş Modunu Başlat” düğmesine bastığında başlatılır.
