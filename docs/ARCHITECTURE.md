# Mimari

Flutter; kullanıcı arayüzü, izin isteme ve tek yönlü durum akışından sorumludur. Kotlin; Nearby, ses rotası, codec, capture/playback ve foreground service yaşam döngüsünü yönetir. Ses frame'leri Dart katmanından geçmez.

Akış: `HomeScreen → RidingSessionController → MethodChannel → RideLinkRuntime`. Native olaylar `EventChannel` ile domain durumuna çevrilir. `RideLinkRuntime`, `NearbyConnectionManager`, `AudioDeviceManager` ve `AudioSessionManager` bileşenlerini koordine eder.

Native ses akışı: `AudioRecord (VOICE_COMMUNICATION) → PCM16/16 kHz/mono/20 ms → MediaCodec Opus 20 kbps → AudioPacket → Nearby BYTES → jitter buffer → MediaCodec Opus decoder → AudioTrack`.

`AudioCodecManager` cihazın gerçek Opus encoder ve decoder MediaCodec desteğini çalışma anında kontrol eder. Destek yoksa PCM'e sessiz fallback yapmaz.
