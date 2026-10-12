# Ses hattı

Format mono PCM16, 16 kHz ve 20 ms (320 sample/640 byte) frame'dir. Hedef Opus bitrate 20 kbps'tir. Binary başlık; magic, sürüm, tür, 128-bit session UUID, 32-bit sequence, 64-bit monotonic timestamp ve 16-bit payload uzunluğu taşır.

Jitter buffer hedefi 3, üst sınırı 10 pakettir. Sequence boşlukları kayıp sayacına eklenir. AEC, NS ve AGC cihazda mevcutsa `AudioRecord` oturumuna bağlanır; gerçek interkom DSP'siyle etkileşimi cihaz testinde doğrulanmalıdır.

API 31+ ses rotası `setCommunicationDevice()` ile seçilir. API 26–30 yalnız ilgili sürümlerde deprecated Bluetooth SCO uyumluluk yolunu kullanır. Bağlı cihaz listesi tek başına mikrofon hazır kanıtı sayılmaz; `AudioRecord` gerçekten başlayamazsa oturum hata verir.

PoC codec'i Android MediaCodec Opus kullanır. Android platformu decoder sunabilse de Opus encoder bütün üreticilerde garanti değildir. Encoder bulunmayan cihazlar açıkça `OPUS_CODEC_UNAVAILABLE` verir; JNI libopus entegrasyonu sonraki stabilizasyon işidir.
