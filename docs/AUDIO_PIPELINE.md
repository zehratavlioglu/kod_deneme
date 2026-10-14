# Ses hattı

Format mono PCM16, 48 kHz ve 20 ms (960 sample/1920 byte) frame'dir. Hedef Opus bitrate 20 kbps'tir. Binary başlık; magic, sürüm, tür, 128-bit session UUID, 32-bit sequence, 64-bit monotonic timestamp ve 16-bit payload uzunluğu taşır.

Jitter buffer hedefi 2, üst sınırı 5 pakettir. Sequence boşlukları kayıp sayacına eklenir. Çıkış tamponu cihazın minimum gereksinimi ile dört ses karesinden büyük olan değerdir. AEC, NS ve AGC cihazda mevcutsa `AudioRecord` oturumuna bağlanır; gerçek interkom DSP'siyle etkileşimi cihaz testinde doğrulanmalıdır.

API 31+ ses rotası `setCommunicationDevice()` ile seçilir. API 26–30 yalnız ilgili sürümlerde deprecated Bluetooth SCO uyumluluk yolunu kullanır. Bağlı cihaz listesi tek başına mikrofon hazır kanıtı sayılmaz; `AudioRecord` gerçekten başlayamazsa oturum hata verir.

PoC codec'i Android MediaCodec Opus kullanır. Konteynersiz canlı akış nedeniyle decoder'a Opus identification header, pre-skip ve seek pre-roll CSD tamponları açıkça verilir. Codec başlatma/durdurma eşzamanlıdır ve oturum generation değeri eski işlerin yeni codec oturumuna erişmesini engeller. Android platformu decoder sunabilse de Opus encoder bütün üreticilerde garanti değildir. Encoder bulunmayan cihazlar açıkça `OPUS_CODEC_UNAVAILABLE` verir; JNI libopus entegrasyonu sonraki stabilizasyon işidir.

Telefonla debug modunda API 31+ cihazlarda `setCommunicationDevice()` ile, eski cihazlarda `isSpeakerphoneOn` ile dış hoparlör zorlanır. Fiziksel testlerde Nothing A063 oynatma hattı yaklaşık 139 ms, Samsung SM-M127F hattı yaklaşık 225 ms sistem gecikmesi bildirmiştir; bunlar cihaz/HAL ölçümleridir ve uçtan uca ağ gecikmesi değildir.
