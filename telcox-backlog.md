# TelcoX — Trello Backlog (Tüm Kartlar)

_Toplam aktif kart: 31 — Kaynak: Trello board export (HlB5t0zm - telcox)_

---

## 📋 Active Sprint Backlog

### KART 1: [IDENTITY] JWT Auth & Redis Blacklist Mekanizması

**Etiketler:** `Identity`, `Customer`, `Critical`, `Mahmut`
**Üyeler:** Mahmut Güneş

**Açıklama**

Kullanıcıların sisteme güvenli giriş yapabilmesi ve çıkış yaptıklarında token'larının geçersiz kılınması için kimlik doğrulama sisteminin kurulması.

**Tasks**

_0% tamamlandı (0/3)_

- [ ] identity-service (9001) üzerinde POST /api/v1/auth/login endpoint'i yazılmalı, başarılı girişte 15 dk süreli Access Token ve 7 gün süreli Refresh Token dönmeli
- [ ] POST /api/v1/auth/refresh isteği geldiğinde eski refresh token Redis'e blacklist olarak yazılmalı (TTL = Access Token süresi) ve yeni token çifti üretilmeli (Token Rotation).
- [ ] Rol ve İzin (Role & Permission) veritabanı tabloları Flyway scripti ile oluşturulmalı ve Spring Security entegrasyonu tamamlanmalı.

---

### KART 2: [CUSTOMER] Müşteri CRUD & TCKN AES-GCM Şifreleme

**Etiketler:** `Critical`
**Üyeler:** Mahmut Güneş

**Açıklama**

KVKK mevzuatına uygun şekilde müşteri bilgilerinin kaydedilmesi, güncellenmesi ve hassas verilerin şifrelenmesi.

**Checklist**

_0% tamamlandı (0/4)_

- [ ] customer-service (9002) üzerinde bireysel müşteri oluşturma (POST /api/v1/customers) ve güncelleme endpoint'leri yazılmalı.
- [ ] Gelen TCKN bilgisi 11 hane ve resmi TCKN algoritmasına göre valide edilmeli.
- [ ] TCKN alanı veritabanına yazılmadan önce JPA AttributeConverter (PiiConverter) kullanılarak AES-GCM algoritması ile şifrelenmeli.
- [ ] DELETE /api/v1/customers/{id} isteği fiziksel silme yapmamalı; KVKK gereği veriyi is_deleted = true olarak işaretlemeli (Soft-delete).

---

### KART 3: [CUSTOMER] KYC Mock Onay Akışı & Debezium Outbox Yapılandırması

**Etiketler:** `Critical`
**Üyeler:** Mahmut Güneş

**Açıklama**

Müşteri döküman onay sürecinin işletilmesi ve veri tabanında oluşan değişikliklerin ağ yükü yaratmadan dış dünyaya duyurulması.

**Checklist**

_0% tamamlandı (0/4)_

- [ ] POST /api/v1/customers/{id}/kyc/approve mock admin endpoint'i yazılmalı, müşteri statüsü PENDING durumundan ACTIVE durumuna çekilmeli.
- [ ] Onaylama anında veritabanındaki outbox_event tablosuna CustomerKYCApprovedEvent kaydı (JSON payload ile) eklenmeli.
- [ ] Uygulama kodunda kesinlikle Kafka Producer kodu yazılmamalı; Debezium CDC PostgreSQL connector'ü WAL (Write-Ahead Log) üzerinden bu kaydı okuyup telcox.customer.customer.CustomerKYCApproved topic'ine otomatik basmalı.
- [ ] Klasik Gateway Yönetimi İzolasyonu: Docker Compose dosyasında api-gateway (8080) haricindeki tüm iç servis portları (9001, 9002 vb.) host makineye expose edilmemeli, servisler sadece iç ağda kalmalı.

---

### KART 4: [CATALOG] Tarife & Ek Paket CRUD ve Immutable Versiyonlama

**Etiketler:** `Medium`
**Üyeler:** Mahmut Güneş

**Açıklama**

Satışa sunulacak tarife ve ek paketlerin admin panelinden yönetilmesi ve mevcut abonelerin fiyat değişimlerinden etkilenmemesi için değişmezlik mimarisinin kurulması.

**Checklist**

_0% tamamlandı (0/4)_

- [ ] product-catalog-service (9003) üzerinde Tariff ve Addon entity'leri için Flyway migration ve temel CRUD endpoint'leri yazılmalı.
- [ ] Tarife tablosunda paket tipleri (POSTPAID, PREPAID, HYBRID) Enum olarak tutulmalı.
- [ ] Mevcut bir tarife güncellendiğinde veya fiyatı değiştiğinde eski DB kaydı ezilmemeli (immutable record); yeni bir satır oluşturularak versiyonlanmalı.
- [ ] Abonelikler tarife kimliği yerine tariffCode üzerinden eşleşmeli ve faturaya o versiyonun snapshot fiyatı yansıtılmalı.

---

### KART 5: [CATALOG] Redis Cache-Aside & Eviction Yapılandırması

**Etiketler:** `Medium`
**Üyeler:** Mahmut Güneş

**Açıklama**

Sıkça sorgulanan tarife listelerinin veritabanına yük bindirmemesi için Redis önbellek mekanizmasının kurulması

**Checklist**

_0% tamamlandı (0/3)_

- [ ] GET /api/v1/tariffs endpoint'i istek aldığında önce Redis'e bakmalı; cache miss durumunda DB'den çekip cache'e yazmalı (catalog:tariff:all anahtarı ile, TTL: 10 dk).
- [ ] GET /api/v1/tariffs/{code} tekil tarife sorgusu catalog:tariff:{code} anahtarı ile 30 dk TTL verilerek cache'lenmeli.
- [ ] Admin yeni bir tarife eklediğinde veya fiyat değiştirdiğinde (TariffPriceChanged tetiklendiğinde) ilgili Redis cache anahtarları otomatik silinmeli (Eviction).

---

### KART 6: [ORDER] Sipariş Altyapısı & SagaState Veri Katmanı Hazırlığı

**Etiketler:** `Medium`
**Üyeler:** Mahmut Güneş

**Açıklama**

Sipariş süreçlerinin durum yönetimini üstlenecek veri katmanının ve dağıtık transaction durum tablosunun kurulması.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] order-service (9004) bünyesinde sipariş ve sipariş kalemleri (invoice_lines kalemi olacak yapılar) tabloları Flyway ile oluşturulmalı.
- [ ] Sipariş durumu DRAFT, PENDING_PAYMENT, PAID, FULFILLED, CANCELLED geçişlerini destekleyen bir durum yapısına sahip olmalı.
- [ ] Dağıtık transaction adımlarını izlemek amacıyla veritabanında SagaState tablosu (fields: sagaId, currentStep, status, payload, lastUpdated) oluşturulmalı.

---

### KART 7: [SUBSCRIPTION] Abonelik State Machine & Redisson Distributed Lock Havuzu

**Etiketler:** `Critical`
**Üyeler:** Osman Bülbül

**Açıklama**

Dağıtık mimaride aynı telefon numarasının aynı anda iki farklı kişiye satılmasını engelleyecek güvenli numara tahsis yapısının kodlanması.

**Checklist**

_100% tamamlandı (3/3)_

- [x] subscription-service (9005) tabanında subscription ve msisdn_pool tabloları Flyway ile kurulmalı. Havuzdaki numaralar FREE, RESERVED, ALLOCATED statülerine sahip olmalı.
- [x] Numara tahsis endpoint'i tetiklendiğinde, Redisson RLock kullanılarak distributed lock alınmalı (Lock Key: lock:msisdn:{number}, Timeout: 5sn).
- [x] Kilit altındayken DB'den FREE numara çekilmeli, statü RESERVED yapılmalı ve lock bırakılmalı. Çakışma riskine karşı DB seviyesinde msisdn alanına UNIQUE constraint eklenmeli.

---

### KART 8: [USAGE] Kota ve Kullanım Veri Modelleri (UsageRecord & Quota)

**Etiketler:** `Medium`
**Üyeler:** Osman Bülbül

**Açıklama**

Abone kullanımlarının (Dakika, SMS, GB) düşüleceği kota sayaç tablosunun ve geçmiş kullanım kayıt veri tabanının kurulması.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] usage-service (9006) bünyesinde quota (alanlar: minutesRemaining, smsRemaining, mbRemaining, periodEnd) tablosu Flyway ile oluşturulmalı.
- [ ] Her çağrı/internet harcamasının ham logunu tutacak usage_record (alanlar: subscriptionId, msisdn, amount, type: VOICE/SMS/DATA, recordedAt) tablosu kurulmalı.
- [ ] GET /api/v1/usage/subscriptions/{id}/quota endpoint'i yazılmalı ve o abonenin kalan anlık haklarını dönmeli.

---

### KART 9: [USAGE] Kafka CdrRecorded Consumer Altyapısı (MSISDN Partition Key)

**Etiketler:** `Critical`
**Üyeler:** Osman Bülbül

**Açıklama**

Simülatörden fırlatılacak ham çağrı kayıtlarının veri kaybı yaşanmadan ve abona bazlı kronolojik sırayla dinleneceği tüketici katmanının yazılması.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] telco.usage.events topic'ini dinleyecek Spring Kafka Consumer bileşeni (CdrRecorded event'i için) kodlanmalı.
- [ ] Mesaj sıralamasının bozulmaması ve aynı abonenin harcamalarının yarış haline girmemesi için Kafka'da partition key olarak kesinlikle MSISDN kullanılmalı.
- [ ] Gelen harcama miktarı kadar ilgili quota satırındaki kalan miktar azaltılmalı ve usage_record tablosuna tarih damgasıyla eklenmeli.

---

### KART 10: [NOTIFICATION] Şablon CRUD & Mailpit Entegrasyonu

**Etiketler:** `Medium`
**Üyeler:** Osman Bülbül

**Açıklama**

Kullanıcılara gönderilecek SMS ve E-posta şablonlarının yönetilmesi ve test ortamında sahte sunucuya yönlendirilmesi.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] notification-service (9007) üzerinde NotificationTemplate entity'si ve şablon ekleme/düzenleme CRUD endpoint'leri yazılmalı.
- [ ] Şablon içerisindeki dinamik alanlar için placeholder (Örn: Merhaba {customerName}) yapısı kurulmalı
- [ ] Gerçek maillerin havada kalmaması için yerel ortam docker-compose konfigürasyonuna sahte mail sunucusu olan Mailpit entegrasyonu dahil edilmeli.

---

### KART 11: [QA] CDR Simulator Geliştirilmesi (MSISDN Key'li)

**Etiketler:** `Critical`
**Üyeler:** Osman Bülbül

**Açıklama**

Sistemi canlıya almadan önce gerçekçi şebeke yükü üretecek, yüksek hızlı bir çağrı simülatör aracının yazılması.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] Bağımsız çalışabilen bir simülatör bileşeni kodlanmalı. Bu bileşen Kafka'nın telco.usage.events topic'ine saniyede 100 adet CdrRecorded event'i basabilmeli.
- [ ] Üretilen event'lerin mesaj anahtarı (Kafka Message Key) alanına partition dağılımının doğru yapılması için kesinlikle MSISDN basılmalı.
- [ ] Simülatörün veri üretim hızı ve hedef msisdn listesi application.yml üzerinden konfigüre edilebilir (dinamik) olmalı.

---

### KART 12: [QA] Hafta 1 Onboarding E2E Test Kurgusu

**Etiketler:** `Medium`
**Üyeler:** Osman Bülbül

**Açıklama**

Hafta 1 sonunda üretilen servislerin (Customer, Order) entegre şekilde ilk temel akışı çalıştırabildiğinin test otomasyonu ile kanıtlanması.

**Checklist**

_0% tamamlandı (0/2)_

- [ ] RestAssured veya Postman Collection kullanılarak POST /customers -> KYC approve -> POST /orders akışını sırayla tetikleyen otomatik test scripti hazırlanmalı.
- [ ] Test sonucunda siparişin başarıyla PENDING_PAYMENT durumuna geçtiği ve Debezium outbox tablosuna sipariş kaydının düştüğü doğrulanmalı.

---

## 📋 Backlog

### KART 13: [GATEWAY] Redis WebFilter Rate Limiting & Auth Relay

**Etiketler:** `Critical`
**Üyeler:** Mahmut Güneş

**Açıklama**

API Gateway katmanında gelen isteklerin güvenli şekilde sınırlandırılması ve çözülen kimlik bilgilerinin iç servislere güvenle taşınması.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] api-gateway üzerinde Redis tabanlı "Sliding Window" algoritması kullanan bir WebFilter yazılmalı; kullanıcı başına dakikada maksimum 100 istek sınırı (HTTP 429) konulmalı.
- [ ] Başarılı login olan kullanıcıların JWT token'ları Gateway'de çözülmeli; kullanıcının benzersiz ID bilgisi X-User-Id ve rolleri X-User-Roles header'ları eklenerek downstream (iç) servislere paslanmalı.
- [ ] Gateway dışındaki servislerin portlarının dış dünyaya tamamen kapalı olduğu (Docker internal network izolasyonu) teyit edilmeli.

---

### KART 14: [OBSERVABILITY] OpenTelemetry, Zipkin ve Prometheus Dağıtık İzleme Setup

**Etiketler:** `Low`
**Üyeler:** Mahmut Güneş

**Açıklama**

Mikroservis mimarisinde bir isteğin hangi servislerden geçip nerede hata aldığını tek bir izleme ekranından görebilme altyapısı

**Checklist**

_0% tamamlandı (0/3)_

- [ ] Projedeki tüm mikroservislere OpenTelemetry bağımlılıkları eklenmeli ve izleme verileri Zipkin container'ına yönlendirilmeli.
- [ ] İstek zincirinin kırılmaması için Gateway katmanında üretilen Correlation-Id veya Trace-Id tüm log satırlarına otomatik enjekte edilmeli.
- [ ] Servislerin /actuator/prometheus metrik uç noktaları aktif edilmeli ve Grafana üzerinde Spring Boot metrik dashboard'u ayağa kaldırılmalı.

---

### KART 15: [ORDER SAGA] Merkezi SagaOrchestrator Uçtan Uca Mutlu Yol (Happy Path)

**Etiketler:** `Critical`
**Üyeler:** Mahmut Güneş

**Açıklama**

Sipariş tamamlama sürecindeki Ödeme ve Abonelik işlemlerini tek bir merkezden yönetecek orkestratör bileşeninin mutlu yol senaryosunun kodlanması.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] order-service içerisinde senkron müşteri ve katalog kontrolleri bittikten sonra çalışacak bir SagaOrchestrator bileşeni yazılmalı.
- [ ] Orkestratör sırasıyla; outbox tablosuna ödeme emri yazmalı -> Ödeme servisinden onay Debezium-Kafka üzerinden gelince SagaState tablosunu STEP_2 yapmalı -> Abonelik servisine aktivasyon emri göndermeli.
- [ ] Abonelik servisinden SubscriptionActivatedEvent başarıyla alındığında sipariş durumu FULFILLED konumuna çekilmeli ve Saga durumu COMPLETED olarak işaretlenmeli.

---

### KART 16: [ORDER SAGA] Kompansasyon (Geri Alma) Akışı ve İstisnai Durum Yönetimi

**Etiketler:** `Critical`
**Üyeler:** Mahmut Güneş

**Açıklama**

Dağıtık transaction adımlarında herhangi bir iç serviste (Örn: Abonelik) hata çıktığında, yapılan finansal işlemleri otomatik tersine çevirecek telafi kodunun yazılması.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] Abonelik katmanından sisteme SubscriptionActivationFailed eventi düştüğünde, SagaOrchestrator bunu yakalamalı ve durumu COMPENSATING yapmalı.
- [ ] Orkestratör otomatik olarak outbox tablosuna PaymentRefundRequested (Para iade emri) yazmalı ve ödeme servisinin parayı iade etmesini tetiklemeli.
- [ ] İade süreci başarıyla bittiğinde sipariş durumu CANCELLED, SagaState durumu ise COMPENSATED olarak kapatılmalı.

---

### KART 17: [ORDER] Resilience4j Circuit Breaker & Retry Yapılandırması

**Etiketler:** `Medium`
**Üyeler:** Mahmut Güneş

**Açıklama**

Sipariş servisinde, dışarıya atılan senkron HTTP/FeignClient çağrılarında (Müşteri doğrulama vb.) karşı servis çöktüğünde sistemin kilitlenmesini önleme.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] order-service içindeki OpenFeign istemcilerine Resilience4j Circuit Breaker konfigürasyonu eklenmeli.
- [ ] Karşı taraf hata oranı %50'yi geçtiğinde devre açılmalı (Open) ve sistem doğrudan fallback metoduna girerek HTTP 503 (Servis geçici olarak servis dışı) dönmeli.
- [ ] Hatalı istekler için application.yml tabanlı 3 denemeli, 500ms exponential backoff (katlanarak artan bekleme süresi) retry kuralı tanımlanmalı.

---

### KART 18: [BILLING] Toplu Fatura Kesim (Bill-run) Zamanlayıcısı & Redisson Dağıtık Kilit

**Etiketler:** `Critical`
**Üyeler:** Osman Bülbül

**Açıklama**

Ay sonu fatura kesim zamanı geldiğinde arka planda çalışan toplu fatura işinin podlar arasında çakışmadan güvenle çalıştırılması.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] billing-service (9008) bünyesinde aktif abonelerin aylık sabit harcamalarını toplayan zamanlanmış görev (@scheduled cron job) kodlanmalı.
- [ ] Aynı anda ayakta olan iki farklı Billing uygulamasının aynı aboneye iki kez fatura kesmesini önlemek için Redisson RLock ile bill-run:{yyyyMM} dağıtık kilidi alınmalı.
- [ ] Oluşan fatura kayıtları outbox tablosuna InvoiceGeneratedEvent olarak eklenmeli ve Debezium ile dış dünyaya fırlatılmalı.

---

### KART 19: [PAYMENT] Idempotency-Key Kontrolü & Akıllı Ödeme Retry Zamanlayıcısı

**Etiketler:** `Critical`
**Üyeler:** Osman Bülbül

**Açıklama**

Ağ kesintisi veya kullanıcının butona çift basması durumunda karttan iki kez para çekilmesinin kesin olarak engellenmesi ve hata alan faturaların otomatik takibi.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] payment-service (9009) gelen POST /api/v1/payments isteklerinde HTTP Header alanında Idempotency-Key verisini zorunlu kılmalı.
- [ ] Gelen key Redis üzerine SET NX (24 saat TTL) ile yazılmalı. Eğer key zaten varsa ödeme POS'a atılmadan doğrudan ilk işlemin cevabı dönülmeli.
- [ ] Başarısız ödemeler için kart hata koduna göre tetiklenen bir Scheduler kurulmalı; ödeme girişimleri sırasıyla 24, 72 ve 168 saatlik periyotlarla otomatik olarak tekrar POS'a gönderilmeli.

---

### KART 20: [BILLING] iText/JasperReports ile Fatura PDF Üretimi & MinIO Depolama

**Etiketler:** `Critical`
**Üyeler:** Osman Bülbül

**Açıklama**

Kesilen faturanın resmi PDF belgesinin üretilmesi ve güvenli şekilde bulut nesne depolama alanında saklanması.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] Debezium'dan gelen InvoiceGeneratedEvent dinlenmeli, iText veya JasperReports kütüphanesi ile abonenin dönem borcunu, vergisini ve detayını içeren fatura PDF'i üretilmeli.
- [ ] Üretilen PDF dosyası kesinlikle lokal disk yerine S3 uyumlu MinIO sunucusundaki telcox-invoices bucket'ı altına invoices/{customerId}/{invoiceId}.pdf dizilimiyle yüklenmeli.
- [ ] GET /api/v1/invoices/{id}/pdf endpoint'i yazılmalı, çağrıldığında MinIO üzerinden sadece 10 dakika geçerli, dışa açık güvenli presigned URL (süreli link) döndürmeli.

---

### KART 21: [TICKET] Destek Talebi Yönetimi & SLA Zaman Aşımı Kontrolü

**Etiketler:** `Low`
**Üyeler:** Osman Bülbül

**Açıklama**

Müşterilerin çağrı merkezine ilettiği arıza ve şikayetlerin durum takibi ve çözüm sürelerinin yasal sınır kontrolü.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] ticket-service (9010) ayağa kaldırılmalı ve destek talebi oluşturma (POST /api/v1/tickets) endpoint'leri yazılmalı.
- [ ] Talep oluşturulurken seçilen öncelik seviyesine (CRITICAL, HIGH, MEDIUM) göre sistem otomatik olarak ileri yönlü bir son çözüm tarihi (slaDueAt) hesaplamalı.
- [ ] Arka planda çalışan bir cron job, süresi dolan ticket'ları tarayıp outbox tablosuna SlaBreachedEvent kaydı atmalı; Debezium bunu okuyup Kafka üzerinden bildirim servisine iletmeli.

---

### KART 22: [QA] Senaryo 14.2 (Aylık Fatura) ve 14.3 (Kota Aşımı) E2E Test Otomasyonu

**Etiketler:** `Medium`
**Üyeler:** Osman Bülbül

**Açıklama**

Fatura kesim döngüsünün ve kota aşım uyarı mekanizmalarının entegre şekilde çalıştığının test kodları ile kanıtlanması.

**Checklist**

_0% tamamlandı (0/2)_

- [ ] 14.2 Testi: POST /billing/runs tetiklenmeli -> Debezium event'i ile MinIO'da PDF oluştuğu doğrulanmalı -> Ödeme endpoint'ine istek atılarak faturanın PAID statüsüne geçtiği assert edilmeli.
- [ ] 14.3 Testi: CDR Simulator çalıştırılarak hedef test msisdn numarası için kota %80 ve %100 limitlerine ulaştırılmalı, Mailpit/Log katmanına doğru şablonlu SMS düştüğü doğrulanmalı.

---

### KART 23: [K8S] Minikube Deployment Manifestleri & K8s Secrets Entegrasyonu

**Etiketler:** `Low`
**Üyeler:** Mahmut Güneş

**Açıklama**

Geliştirilen tüm servislerin lokal bilgisayardaki Docker ortamından çıkarılarak Kubernetes kümesine taşınması.

**Checklist**

_0% tamamlandı (0/2)_

- [ ] Geliştirilen her mikroservis için K8s ortamına uygun deployment.yaml ve service.yaml manifest dosyaları yazılmalı.
- [ ] Veritabanında TCKN alanlarını şifrelemek için kullanılan gizli AES-GCM anahtarı kaynak kod içerisinden tamamen silinmeli ve K8s Secrets nesnesi olarak tanımlanıp podlara enjekte edilmeli.

---

### KART 24: [SECURITY] Yatay Ölçekleme (HPA) Konfigürasyonu & mTLS Analiz Raporu

**Etiketler:** `Low`
**Üyeler:** Mahmut Güneş

**Açıklama**

Sistemin anlık gelen yoğun yüklere karşı otomatik pod sayısını artırmasını sağlayacak kuralların girilmesi.

**Checklist**

_0% tamamlandı (0/2)_

- [ ] Yoğun harcama verisi işleyen usage-service ve fatura dönemi yüklenen billing-service podları için CPU tüketimi %70'i geçtiğinde çalışacak yatay ölçekleyici (HPA) manifesti girilmeli (Min: 1, Max: 3 pod).
- [ ] Servislerin iç ağda birbirleriyle şifreli konuşması (mTLS) mimari açıdan araştırılmalı ve teslim dökümanına MVP analiz notu olarak eklenmeli (Kodlama yapılmayacak).

---

### KART 25: [ORDER] Ek Paket (Addon) Satın Alma & Tarife Değişikliği İş Akışları

**Etiketler:** `Low`
**Üyeler:** Mahmut Güneş

**Açıklama**

Kullanıcıların mevcut taahhütleri devam ederken ek internet paketi alabilmesi veya tarife yükseltme süreçlerinin iş kurallarının yazılması.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] Aktif aboneliğin üzerine POST /api/v1/orders/addons uç noktası ile ek paket sipariş akışı eklenmeli ve orkestratör üzerinden ödeme adımı çalıştırılmalı.
- [ ] Tarife geçiş isteği geldiğinde eski kota kaydı kapatılmalı, yeni kota tanımlanmalı ve aradaki fiyat farkı bir sonraki fatura dönemine yansıtılmak üzere Billing servisine bildirilmeli.
- [ ] SagaState tablosundaki lastUpdated zaman damgasına bakarak 5 dakikadan uzun süredir askıda kalan işlemleri bulan bir cron job yazılmalı, yöneticiler için manuel compensate tetikleme endpoint'i açılmalı.

---

### KART 26: [TEST] Order ↔ Payment Servisleri Arası Sözleşme (Contract) Testleri

**Etiketler:** `Low`
**Üyeler:** Mahmut Güneş

**Açıklama**

İki servis arasındaki event şemalarından biri değiştiğinde sistemin derleme anında patlayarak hatayı önceden yakalamasını sağlayan koruma testleri.

**Checklist**

_0% tamamlandı (0/2)_

- [ ] Spring Cloud Contract kütüphanesi kullanılarak order-service (Consumer) ve payment-service (Producer) arasındaki event şemaları için sözleşme (groovy/yaml contract) tanımlanmalı.
- [ ] Ödeme servisinin ürettiği PaymentCompleted payload'ı değiştiğinde, sipariş servisinin testleri derleme aşamasında (build time) başarısız olmalı ve hata fırlatmalı.

---

### KART 27: [BILLING] Kota Aşım (Overage) Ücretlendirmesi & Harcama Kalemleri

**Etiketler:** `Medium`
**Üyeler:** Osman Bülbül

**Açıklama**

Kotasını bitiren abonenin simülatörden gelen ekstra kullanımlarının (aşım miktarı) hesaplanarak faturaya ek satır harcaması olarak eklenmesi.

**Checklist**

_0% tamamlandı (0/2)_

- [ ] QuotaExceeded (%100 kota aşımı) durumu oluştuktan sonra usage-service tarafından gelen yeni kullanım verileri billing_extra (aşım) flag'i ile veritabanına işaretlenmeli
- [ ] Bill-run job çalıştığında bu harcamalar BigDecimal veri tipi üzerinden hesaplanmalı ve ana faturanın altına InvoiceLine (aşım kalemi) olarak eklenmeli (Para birimi kesinlikle TRY olmalı).

---

### KART 28: [PAYMENT] Dijital Cüzdan (Wallet Balance) Sistemi & Ödeme Önceliği

**Etiketler:** `Medium`
**Üyeler:** Osman Bülbül

**Açıklama**

Müşterilerin sistemde bakiye tutabilmesi ve fatura ödeme anında kredi kartından önce bu bakiyenin harcanması altyapısı.

**Checklist**

_0% tamamlandı (0/2)_

- [ ] payment-service içerisine müşteri bazlı walletBalance alanı Flyway ile eklenmeli ve cüzdana para yükleme (POST /api/v1/payments/wallet/top-up) endpoint'i yazılmalı.
- [ ] Fatura ödeme tetiklendiğinde sistem önce abonenin cüzdan bakiyesini kontrol etmeli; bakiye fatura tutarına yetiyorsa doğrudan cüzdandan düşmeli, yetmiyorsa kalan tutar için sanal POS (PSP Mock) çağrısı yapmalı.

---

### KART 29: [PERFORMANCE] k6/JMeter ile 1K Kullanıcı Bill-run Yük Testi

**Etiketler:** `Medium`
**Üyeler:** Osman Bülbül

**Açıklama**

Sistemin canlıya çıkmadan önce toplu fatura kesim esnasındaki performans limitlerinin ve hızının ölçülmesi.

**Checklist**

_0% tamamlandı (0/2)_

- [ ] k6 veya Apache JMeter aracı kullanılarak veritabanında hazır bulunan 1000 sanal abone verisi üzerinde toplu fatura kesim senaryosu koşturulmalı.
- [ ] Yapılan performans testinde 1000 abonenin fatura kesim, outbox yazım ve PDF üretim adımlarının tamamı 5 dakikanın altında (p95 < 5dk) sonuçlanmalı ve test raporu proje dosyalarına eklenmeli.

---

### KART 30: [GATEWAY] Merkezi Swagger UI Agregasyonu

**Etiketler:** `Medium`
**Üyeler:** Mahmut Güneş

**Açıklama**

Geliştiricilerin ve test ekiplerinin tüm servislerin API dökümanlarına tek bir web arayüzünden erişebilmesinin sağlanması.

**Checklist**

_0% tamamlandı (0/2)_

- [ ] api-gateway (8080) üzerinde OpenAPI konfigurasyonu yapılmalı.
- [ ] Kullanıcı tarayıcıdan http://localhost:8080/swagger-ui.html adresine girdiğinde, sağ üstteki açılır menüden (identity, customer, billing vb.) tüm iç servislerin endpoint dokümanlarını tek ekranda görebilmeli (Swagger Agregasyonu).

---

### KART 31: [QA] 3 Temel Kabul Senaryosunun Nihai Doğrulanması & Demo Raporu

**Etiketler:** `Critical`
**Üyeler:** Osman Bülbül

**Açıklama**

MVP teslimatı öncesinde iş birimlerine sunulacak olan 3 ana senaryonun hatasız çalıştığının kanıtlanması ve kapanış regresyon testi.

**Checklist**

_0% tamamlandı (0/3)_

- [ ] 14.1 (Onboarding), 14.2 (Aylık Fatura) ve 14.3 (Kota Aşımı) kabul senaryoları entegrasyon test suitinde sırayla koşturulmalı ve %100 başarı (yeşil) raporu üretilmeli.
- [ ] Tüm servislerin local deployment adımlarını, docker-compose komutlarını ve gerekli .env değişkenlerini listeleyen eksiksiz bir README.md dökümanı hazırlanmalı.
- [ ] Postman Collection Runner aracılığıyla senaryoların arka arkaya hatasız aktığını gösteren kısa bir teknik ekran kaydı (Demo Videosu) çekilerek teslimat paketine eklenmeli ve genel regresyon koşusu başarıyla bitirilmeli.

---
