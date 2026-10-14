# Nearby protokolü

Servis kimliği `com.lideatech.ridelink`, strateji `P2P_POINT_TO_POINT`, şema sürümü `1`'dir. Kontrol mesajları UTF‑8 JSON `Payload.BYTES`, ses mesajları ise uzun ömürlü ve length-prefix çerçeveli `Payload.STREAM` olarak taşınır.

Kontrol tipleri: `HELLO`, `CAPABILITIES`, `MATCH_REQUEST`, `MATCH_ACCEPT`, `MATCH_REJECT`, `SESSION_START`, `AUDIO_START`, `AUDIO_STOP`, `HEARTBEAT`, `DISCONNECT`.

Her sürüş oturumu rastgele 16 karakterlik bir ilan tokenı oluşturur. Her iki telefon aynı anda advertising ve discovery yapar; yalnızca leksikografik olarak küçük token bağlantıyı başlatır. Böylece simetrik istek yarışı önlenir. Oturum UUID'si sıralanmış iki token'dan deterministik üretilir.

Nearby authentication rakamları kullanıcı olay akışına sunulur. Mevcut PoC açık otomatik-eşleşme rızası varsa bağlantıyı otomatik kabul eder. Bu, ilk karşılaşmada MITM/yanlış kişi riskini tamamen gidermez; production için güvenilir eş listesi veya manuel doğrulama gerekir.

Her ses yönünde tek STREAM oluşturulur. STREAM içindeki her kayıt 32-bit frame uzunluğu ve ardından `AudioPacket` baytlarından oluşur; 8 KiB üzerindeki frame reddedilir. Gönderici tarafındaki üç frame'lik kuyruk dolarsa en eski frame atılır. Böylece yavaş cihazlarda güvenilir BYTES teslimat kuyruğunun büyüyüp konuşmayı saniyeler geriden oynatması önlenir.
