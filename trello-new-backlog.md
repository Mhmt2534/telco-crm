Harika bir karar! Altyapı kurulumunu (Maven Multi-Module, Config/Discovery Server, API Gateway iskeleti, Docker Compose temeli) zaten **tamamlanmış** kabul ediyoruz ve takımı bu yükten kurtarıyoruz.

Aşağıda, junior yazılımcıların kafasında hiçbir soru işareti bırakmayacak şekilde atomik parçalara bölünmüş, **Debezium CDC**, **Kafka MSISDN Partition Key**, **MinIO**, **Saga Orchestration** ve **Gateway Ağ İzolasyonu** mimari kararlarını içeren, tam istediğin detay seviyesindeki nihai Trello Backlog planı yer almaktadır:

---

## 🟢 LİSTE: TAMAMLANANLAR (DONE)

### KART 00: [INFRASTRUCTURE] Hazır Ortak Altyapı Bileşenleri

* **Açıklama:** Geliştirme öncesi ihtiyaç duyulan ortak mimari yapılar ayağa kaldırılmıştır. Yeni yazılacak servislerde bu modüller dependency olarak çekilecektir.


* **Mevcut Bileşenler:**
* `common-core`, `common-web` ve `common-persistence` paketleri hazır.


* `api-gateway` (Port: 8080), `discovery-server` (Port: 8761) ve `config-server` (Port: 8888) modülleri çalışır durumda.


* Altyapı servislerini (PostgreSQL 16, Kafka 3.7 KRaft, Redis 7, MinIO, Zipkin) içeren merkezi `docker-compose.yml` dosyası hazır.





---

## 📌 LİSTE: SPRINT 1 — HAFTA 1: Temel Servisler & Veri Katmanı

---
### KART 0.1: [INFRASTRUCTURE] Keycloak Ortak Altyapı Kurulumu (GÜNCELLENDİ)

* **Açıklama:** Mevcut Docker Compose altyapısına Keycloak sunucusunun eklenmesi ve projeye özel rollerin ayarlanması.

* **Atanan Kişi:** Mahmut · Tech Lead


* **Öncelik:** Yüksek



* **Kabul Kriterleri (Checklist):**
* [ ] Merkezi docker-compose.yml dosyasına Keycloak imajı eklenmeli ve PostgreSQL veritabanına bağlanacak şekilde yapılandırılmalı.


* [ ] Keycloak admin paneline girilerek proje için yeni bir Realm (Örn: telco-crm-realm) oluşturulmalı.


* [ ] Mikroservislerin konuşacağı bir Client oluşturulup gizli anahtarı (Client Secret) .env dosyasına alınmalı ya da windows da setx ile tutulmalı.


* [ ] Projenin gerçek iş kurallarına uygun olarak ADMIN, DEALER (Saha Bayisi) ve MUSTERI rolleri Keycloak arayüzünden Realm Roles olarak tanımlanmalı.



---

### KART 01: [IDENTITY] İkili Login Akışı & OTP Entegrasyonu (GÜNCELLENDİ)

* **Açıklama:** Admin/Bayi personeli ile normal müşterilerin farklı yöntemlerle (Şifre vs. OTP) sisteme giriş yapabilmesi için kimlik doğrulama servisinin yazılması.


* **Atanan Kişi:** Mahmut · Tech Lead


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 0.1


* **Kabul Kriterleri (Checklist):**
* [ ] Özel JWT üretme ve Redis blacklist kodları tamamen projeden kaldırılmalı.


* [ ] Admin/Dealer Girişi: Kullanıcı adı ve şifre ile giriş yapan personeller için Keycloak'un Direct Access Grant (Doğrudan Erişim) akışına proxy görevi gören bir login endpoint'i yazılmalı.


* [ ] Müşteri OTP Girişi (Adım 1): Müşteriler için `POST /api/v1/auth/otp/request` endpoint'i yazılmalı (Sisteme kayıtlı telefon numarasına OTP kodu üretip dönmeli/göndermeli).


* [ ] Müşteri OTP Girişi (Adım 2): `POST /api/v1/auth/otp/verify` endpoint'i ile girilen kod doğrulanmalı. Doğrulama başarılı olursa, Identity Service arka planda Keycloak ile konuşarak (Client Credentials veya Token Exchange üzerinden) o müşteriye ait JWT token'ı üretip istemciye (frontend/mobil) dönmeli.




### KART 02: [CUSTOMER] Müşteri CRUD & TCKN AES-GCM Şifreleme

* **Açıklama:** Saha bayisinin (DEALER) müşteri adına başvuru açması ve verilerin güvenli şekilde veritabanına kaydedilmesi. Bu aşamada Keycloak'a kesinlikle dokunulmaz.


* **Atanan Kişi:** Mahmut · Tech Lead


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 01


* **Kabul Kriterleri (Checklist):**
* [ ] `customer-service` üzerinde saha bayisi tarafından tetiklenecek POST /api/v1/customers (Başvuru Açma) endpoint'i yazılmalı.


* [ ] Gelen müşteri verisi sadece PostgreSQL veritabanına kaydedilmeli (Müşteri henüz KYC onayından geçmediği için login olamaz, bu yüzden Keycloak'ta kullanıcı açılmamalıdır).


* [ ] TCKN alanı veritabanına yazılmadan önce JPA AttributeConverter (`PiiConverter`) kullanılarak AES-GCM algoritması ile şifrelenmeli.


* [ ] `DELETE /api/v1/customers/{id}` isteği fiziksel silme yapmamalı; KVKK gereği veriyi `is_deleted = true` olarak işaretlemeli (Soft-delete).




* **Kullanılacak Teknolojiler:** Spring Data JPA, AES-GCM (Java Cryptography Architecture), Jakarta Bean Validation



### KART 03: [CUSTOMER] KYC Onayı & Keycloak Kullanıcı Senkronizasyonu (YENİ)

* **Açıklama:** Saha bayisi tarafından açılan müşteri başvurusunun admin onayından geçmesi, onay anında müşterinin Keycloak'ta bir kimlik kazanması (böylece OTP ile giriş yapabilir hale gelmesi) ve bu değişikliğin Debezium ile dış dünyaya duyurulması.


* **Atanan Kişi:** Mahmut · Tech Lead


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 02


* **Kabul Kriterleri (Checklist):**
* [ ] `POST /api/v1/customers/{id}/kyc/approve` admin endpoint'i yazılmalı, müşteri statüsü `PENDING` durumundan `ACTIVE` durumuna çekilmeli (orijinal davranış korunur).


* [ ] Statü `ACTIVE`'e çekilmeden hemen önce, `customer-service` içinde `keycloak-admin-client` kullanılarak Keycloak Admin API'ye istek atılmalı ve bu müşterinin telefon numarası `username` olacak şekilde Keycloak'ta yeni bir User oluşturulmalı (rol: `CUSTOMER`).

* [ ] Keycloak'tan dönen benzersiz kullanıcı kimliği (`Keycloak User ID`), müşteri veritabanı `kaydına keycloakUserId` alanı olarak yazılmalı. Bu alan, ileride OTP doğrulandığında "bu telefon numarasına karşılık gelen Keycloak kullanıcısı kim" sorusunu cevaplamak için kullanılacak.

* [ ] Keycloak User oluşturma işlemi başarısız olursa (örneğin Keycloak o an erişilemezse), `ACTIVE` statüsüne geçiş de yapılmamalı; işlem bir transaction gibi davranmalı (ya ikisi de başarılı, ya ikisi de geri alınır). Bunun için basit bir try-catch + rollback yeterli, dağıtık transaction (Saga) gerekmiyor çünkü ikisi de aynı serviste tetikleniyor

* [ ] Onaylama anında veritabanındaki `outbox_event` tablosuna `CustomerKYCApprovedEvent` kaydı (JSON payload'a artık `keycloakUserId` de dahil edilerek) eklenmeli.


* [ ] Uygulama kodunda kesinlikle Kafka Producer kodu yazılmamalı; `Debezium CDC` PostgreSQL connector'ü WAL üzerinden bu kaydı okuyup `telcox.customer.customer.CustomerKYCApproved` topic'ine otomatik basmalı (orijinal davranış korunur).


* [ ] Klasik Gateway Yönetimi İzolasyonu: Docker Compose dosyasında `api-gateway` (8080) haricindeki tüm iç servis portları host makineye expose edilmemeli (orijinal davranış korunur).




* **Kullanılacak Teknolojiler:** Debezium PostgreSQL Connector, PostgreSQL WAL Logical Replication, Kafka Connect, Keycloak Admin REST Client



### KART 04: [CATALOG] Tarife & Ek Paket CRUD ve Immutable Versiyonlama

* **Açıklama:** Satışa sunulacak tarife ve ek paketlerin admin panelinden yönetilmesi ve mevcut abonelerin fiyat değişimlerinden etkilenmemesi için değişmezlik mimarisinin kurulması.


* **Atanan Kişi:** Mahmut · Core Flow


* **Öncelik:** Orta


* **Bağımlılık:** KART 01


* **Kabul Kriterleri (Checklist):**
* [ ] `product-catalog-service` (9003) üzerinde `Tariff` ve `Addon` entity'leri için Flyway migration ve temel CRUD endpoint'leri yazılmalı.


* [ ] Tarife tablosunda paket tipleri (`POSTPAID`, `PREPAID`, `HYBRID`) Enum olarak tutulmalı.


* [ ] Mevcut bir tarife güncellendiğinde veya fiyatı değiştiğinde eski DB kaydı ezilmemeli (`immutable record`); yeni bir satır oluşturularak versiyonlanmalı.


* [ ] Abonelikler tarife kimliği yerine `tariffCode` üzerinden eşleşmeli ve faturaya o versiyonun snapshot fiyatı yansıtılmalı.




* **Kullanılacak Teknolojiler:** Spring Boot 3.3.x, Flyway, PostgreSQL 16, MapStruct



### KART 05: [CATALOG] Redis Cache-Aside & Eviction Yapılandırması

* **Açıklama:** Sıkça sorgulanan tarife listelerinin veritabanına yük bindirmemesi için Redis önbellek mekanizmasının kurulması.


* **Atanan Kişi:** Mahmut · Core Flow


* **Öncelik:** Orta


* **Bağımlılık:** KART 04


* **Kabul Kriterleri (Checklist):**
* [ ] `GET /api/v1/tariffs` endpoint'i istek aldığında önce Redis'e bakmalı; cache miss durumunda DB'den çekip cache'e yazmalı (`catalog:tariff:all` anahtarı ile, TTL: 10 dk).


* [ ] `GET /api/v1/tariffs/{code}` tekil tarife sorgusu `catalog:tariff:{code}` anahtarı ile 30 dk TTL verilerek cache'lenmeli.


* [ ] Admin yeni bir tarife eklediğinde veya fiyat değiştirdiğinde (`TariffPriceChanged` tetiklendiğinde) ilgili Redis cache anahtarları otomatik silinmeli (Eviction).




* **Kullanılacak Teknolojiler:** Spring Cache, Redis 7, Spring Boot Actuator



### KART 06: [ORDER] Sipariş Altyapısı & SagaState Veri Katmanı Hazırlığı

* **Açıklama:** Sipariş süreçlerinin durum yönetimini üstlenecek veri katmanının ve dağıtık transaction durum tablosunun kurulması.


* **Atanan Kişi:** Mahmut · Core Flow


* **Öncelik:** Orta


* **Bağımlılık:** KART 02


* **Kabul Kriterleri (Checklist):**
* [ ] `order-service` (9004) bünyesinde sipariş ve sipariş kalemleri (`invoice_lines` kalemi olacak yapılar) tabloları Flyway ile oluşturulmalı.


* [ ] Sipariş durumu `DRAFT`, `PENDING_PAYMENT`, `PAID`, `FULFILLED`, `CANCELLED` geçişlerini destekleyen bir durum yapısına sahip olmalı.


* [ ] Dağıtık transaction adımlarını izlemek amacıyla veritabanında `SagaState` tablosu (fields: `sagaId`, `currentStep`, `status`, `payload`, `lastUpdated`) oluşturulmalı.




* **Kullanılacak Teknolojiler:** Spring Data JPA, PostgreSQL 16, Flyway



### KART 07: [SUBSCRIPTION] Abonelik State Machine & Redisson Distributed Lock Havuzu

* **Açıklama:** Dağıtık mimaride aynı telefon numarasının aynı anda iki farklı kişiye satılmasını engelleyecek güvenli numara tahsis yapısının kodlanması.


* **Atanan Kişi:** Osman · Finans


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 01


* **Kabul Kriterleri (Checklist):**
* [ ] `subscription-service` (9005) tabanında `subscription` ve `msisdn_pool` tabloları Flyway ile kurulmalı. Havuzdaki numaralar `FREE`, `RESERVED`, `ALLOCATED` statülerine sahip olmalı.


* [ ] Numara tahsis endpoint'i tetiklendiğinde, `Redisson RLock` kullanılarak distributed lock alınmalı (Lock Key: `lock:msisdn:{number}`, Timeout: 5sn).


* [ ] Kilit altındayken DB'den `FREE` numara çekilmeli, statü `RESERVED` yapılmalı ve lock bırakılmalı. Çakışma riskine karşı DB seviyesinde `msisdn` alanına `UNIQUE constraint` eklenmeli.




* **Kullanılacak Teknolojiler:** Redisson 3.x, Redis 7, Spring Data JPA, PostgreSQL



### KART 08: [USAGE] Kota ve Kullanım Veri Modelleri (UsageRecord & Quota)

* **Açıklama:** Abone kullanımlarının (Dakika, SMS, GB) düşüleceği kota sayaç tablosunun ve geçmiş kullanım kayıt veri tabanının kurulması.


* **Atanan Kişi:** Osman · Finans


* **Öncelik:** Orta


* **Bağımlılık:** KART 07


* **Kabul Kriterleri (Checklist):**
* [ ] `usage-service` (9006) bünyesinde `quota` (alanlar: `minutesRemaining`, `smsRemaining`, `mbRemaining`, `periodEnd`) tablosu Flyway ile oluşturulmalı.


* [ ] Her çağrı/internet harcamasının ham logunu tutacak `usage_record` (alanlar: `subscriptionId`, `msisdn`, `amount`, `type: VOICE/SMS/DATA`, `recordedAt`) tablosu kurulmalı.


* [ ] `GET /api/v1/usage/subscriptions/{id}/quota` endpoint'i yazılmalı ve o abonenin kalan anlık haklarını dönmeli.




* **Kullanılacak Teknolojiler:** Spring Data JPA, Flyway, PostgreSQL 16



### KART 09: [USAGE] Kafka CdrRecorded Consumer Altyapısı (MSISDN Partition Key)

* **Açıklama:** Simülatörden fırlatılacak ham çağrı kayıtlarının veri kaybı yaşanmadan ve abona bazlı kronolojik sırayla dinleneceği tüketici katmanının yazılması.


* **Atanan Kişi:** Osman · Finans


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 08


* **Kabul Kriterleri (Checklist):**
* [ ] `telco.usage.events` topic'ini dinleyecek Spring Kafka Consumer bileşeni (`CdrRecorded` event'i için) kodlanmalı.


* [ ] Mesaj sıralamasının bozulmaması ve aynı abonenin harcamalarının yarış haline girmemesi için Kafka'da partition key olarak kesinlikle **MSISDN** kullanılmalı.


* [ ] Gelen harcama miktarı kadar ilgili `quota` satırındaki kalan miktar azaltılmalı ve `usage_record` tablosuna tarih damgasıyla eklenmeli.




* **Kullanılacak Teknolojiler:** Spring Kafka, Apache Kafka 3.7 (KRaft mode)



### KART 10: [NOTIFICATION] Şablon CRUD & Mailpit Entegrasyonu

* **Açıklama:** Kullanıcılara gönderilecek SMS ve E-posta şablonlarının yönetilmesi ve test ortamında sahte sunucuya yönlendirilmesi.


* **Atanan Kişi:** Osman · İletişim & QA


* **Öncelik:** Orta


* **Bağımlılık:** KART 01


* **Kabul Kriterleri (Checklist):**
* [ ] `notification-service` (9007) üzerinde `NotificationTemplate` entity'si ve şablon ekleme/düzenleme CRUD endpoint'leri yazılmalı.


* [ ] Şablon içerisindeki dinamik alanlar için placeholder (Örn: `Merhaba {customerName}`) yapısı kurulmalı.


* [ ] Gerçek maillerin havada kalmaması için yerel ortam docker-compose konfigürasyonuna sahte mail sunucusu olan `Mailpit` entegrasyonu dahil edilmeli.




* **Kullanılacak Teknolojiler:** Spring Boot 3.3.x, Mailpit Docker Image, Spring Boot Starter Mail



### KART 11: [QA] CDR Simulator Geliştirilmesi (MSISDN Key'li)

* **Açıklama:** Sistemi canlıya almadan önce gerçekçi şebeke yükü üretecek, yüksek hızlı bir çağrı simülatör aracının yazılması.


* **Atanan Kişi:** Osman · İletişim & QA


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 09


* **Kabul Kriterleri (Checklist):**
* [ ] Bağımsız çalışabilen bir simülatör bileşeni kodlanmalı. Bu bileşen Kafka'nın `telco.usage.events` topic'ine saniyede 100 adet `CdrRecorded` event'i basabilmeli.


* [ ] Üretilen event'lerin mesaj anahtarı (Kafka Message Key) alanına partition dağılımının doğru yapılması için kesinlikle **MSISDN** basılmalı.


* [ ] Simülatörün veri üretim hızı ve hedef msisdn listesi `application.yml` üzerinden konfigüre edilebilir (dinamik) olmalı.




* **Kullanılacak Teknolojiler:** Spring Boot, Spring Kafka (Producer katmanı)



### KART 12: [QA] Hafta 1 Onboarding E2E Test Kurgusu

* **Açıklama:** Hafta 1 sonunda üretilen servislerin (Customer, Order) entegre şekilde ilk temel akışı çalıştırabildiğinin test otomasyonu ile kanıtlanması.


* **Atanan Kişi:** Osman · İletişim & QA


* **Öncelik:** Orta


* **Bağımlılık:** KART 03, KART 06


* **Kabul Kriterleri (Checklist):**
* [ ] RestAssured veya Postman Collection kullanılarak `POST /customers` -> `KYC approve` -> `POST /orders` akışını sırayla tetikleyen otomatik test scripti hazırlanmalı.


* [ ] Test sonucunda siparişin başarıyla `PENDING_PAYMENT` durumuna geçtiği ve Debezium outbox tablosuna sipariş kaydının düştüğü doğrulanmalı.




* **Kullanılacak Teknolojiler:** RestAssured, JUnit 5, Postman Runner



---

## 📌 LİSTE: SPRINT 2 — HAFTA 2: İş Akışları & Entegrasyon

### KART 13: [GATEWAY] Redis WebFilter Rate Limiting & Auth Relay

* **Açıklama:** API Gateway katmanının Keycloak'tan gelen token'ları doğrulayacak şekilde ayarlanması ve iç servislere kimlik paslaması.


* **Atanan Kişi:** Mahmut · Tech Lead


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 01, KART 05


* **Kabul Kriterleri (Checklist):**
* [ ] `api-gateway` projesinin application.yml dosyasına Keycloak'un Issuer URI (Örn: `http://localhost:8080/realms/telco-crm-realm`) adresi eklenmeli.


* [ ] Gateway'e gelen her istekteki JWT token, Keycloak'un public key'i (JWKS) üzerinden Spring Security tarafından otomatik doğrulanmalı (Custom filtre yazmaya gerek kalmayacak).


* [ ] Başarılı doğrulanan token'ın içerisinden (Payload/Claims) kullanıcının sub (Subject ID) bilgisi alınmalı ve iç servislere `X-User-Id` header'ı olarak paslanmalı (Auth Relay)


* [ ] `Redis` tabanlı 100 req/dk Rate Limiting mekanizması orijinal plandaki gibi aynen korunmalı.



* **Kullanılacak Teknolojiler:** Spring Cloud Gateway, Reactive Redis Starter, Gateway WebFilter



### KART 14: [OBSERVABILITY] OpenTelemetry, Zipkin ve Prometheus Dağıtık İzleme Setup

* **Açıklama:** Mikroservis mimarisinde bir isteğin hangi servislerden geçip nerede hata aldığını tek bir izleme ekranından görebilme altyapısı.


* **Atanan Kişi:** Mahmut · Tech Lead


* **Öncelik:** Düşük


* **Bağımlılık:** KART 13


* **Kabul Kriterleri (Checklist):**
* [ ] Projedeki tüm mikroservislere `OpenTelemetry` bağımlılıkları eklenmeli ve izleme verileri `Zipkin` container'ına yönlendirilmeli.


* [ ] İstek zincirinin kırılmaması için Gateway katmanında üretilen `Correlation-Id` veya `Trace-Id` tüm log satırlarına otomatik enjekte edilmeli.


* [ ] Servislerin `/actuator/prometheus` metrik uç noktaları aktif edilmeli ve Grafana üzerinde Spring Boot metrik dashboard'u ayağa kaldırılmalı.




* **Kullanılacak Teknolojiler:** OpenTelemetry Java Agent, Micrometer, Zipkin, Prometheus, Grafana, Loki



### KART 15: [ORDER SAGA] Merkezi SagaOrchestrator Uçtan Uca Mutlu Yol (Happy Path)

* **Açıklama:** Sipariş tamamlama sürecindeki Ödeme ve Abonelik işlemlerini tek bir merkezden yönetecek orkestratör bileşeninin mutlu yol senaryosunun kodlanması.


* **Atanan Kişi:** Mahmut · Core Flow


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 06, KART 07


* **Kabul Kriterleri (Checklist):**
* [ ] `order-service` içerisinde senkron müşteri ve katalog kontrolleri bittikten sonra çalışacak bir `SagaOrchestrator` bileşeni yazılmalı.


* [ ] Orkestratör sırasıyla; outbox tablosuna ödeme emri yazmalı -> Ödeme servisinden onay Debezium-Kafka üzerinden gelince `SagaState` tablosunu `STEP_2` yapmalı -> Abonelik servisine aktivasyon emri göndermeli.


* [ ] Abonelik servisinden `SubscriptionActivatedEvent` başarıyla alındığında sipariş durumu `FULFILLED` konumuna çekilmeli ve Saga durumu `COMPLETED` olarak işaretlenmeli.




* **Kullanılacak Teknolojiler:** Spring Kafka, Debezium CDC, Apache Kafka 3.7



### KART 16: [ORDER SAGA] Kompansasyon (Geri Alma) Akışı ve İstisnai Durum Yönetimi

* **Açıklama:** Dağıtık transaction adımlarında herhangi bir iç serviste (Örn: Abonelik) hata çıktığında, yapılan finansal işlemleri otomatik tersine çevirecek telafi kodunun yazılması.


* **Atanan Kişi:** Mahmut · Core Flow


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 15


* **Kabul Kriterleri (Checklist):**
* [ ] Abonelik katmanından sisteme `SubscriptionActivationFailed` eventi düştüğünde, `SagaOrchestrator` bunu yakalamalı ve durumu `COMPENSATING` yapmalı.


* [ ] Orkestratör otomatik olarak outbox tablosuna `PaymentRefundRequested` (Para iade emri) yazmalı ve ödeme servisinin parayı iade etmesini tetiklemeli.


* [ ] İade süreci başarıyla bittiğinde sipariş durumu `CANCELLED`, `SagaState` durumu ise `COMPENSATED` olarak kapatılmalı.




* **Kullanılacak Teknolojiler:** Spring Data JPA, PostgreSQL 16, Debezium CDC, Kafka Consumer



### KART 17: [ORDER] Resilience4j Circuit Breaker & Retry Yapılandırması

* **Açıklama:** Sipariş servisinde, dışarıya atılan senkron HTTP/FeignClient çağrılarında (Müşteri doğrulama vb.) karşı servis çöktüğünde sistemin kilitlenmesini önleme.


* **Atanan Kişi:** Mahmut · Core Flow


* **Öncelik:** Orta


* **Bağımlılık:** KART 15


* **Kabul Kriterleri (Checklist):**
* [ ] `order-service` içindeki OpenFeign istemcilerine `Resilience4j` Circuit Breaker konfigürasyonu eklenmeli.


* [ ] Karşı taraf hata oranı %50'yi geçtiğinde devre açılmalı (Open) ve sistem doğrudan fallback metoduna girerek HTTP 503 (Servis geçici olarak servis dışı) dönmeli.


* [ ] Hatalı istekler için application.yml tabanlı 3 denemeli, 500ms exponential backoff (katlanarak artan bekleme süresi) retry kuralı tanımlanmalı.




* **Kullanılacak Teknolojiler:** Spring Cloud OpenFeign, Resilience4j Starter



### KART 18: [BILLING] Toplu Fatura Kesim (Bill-run) Zamanlayıcısı & Redisson Dağıtık Kilit

* **Açıklama:** Ay sonu fatura kesim zamanı geldiğinde arka planda çalışan toplu fatura işinin podlar arasında çakışmadan güvenle çalıştırılması.


* **Atanan Kişi:** Osman · Finans


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 08, KART 09


* **Kabul Kriterleri (Checklist):**
* [ ] `billing-service` (9008) bünyesinde aktif abonelerin aylık sabit harcamalarını toplayan zamanlanmış görev (`@Scheduled` cron job) kodlanmalı.


* [ ] Aynı anda ayakta olan iki farklı Billing uygulamasının aynı aboneye iki kez fatura kesmesini önlemek için `Redisson RLock` ile `bill-run:{yyyyMM}` dağıtık kilidi alınmalı.


* [ ] Oluşan fatura kayıtları outbox tablosuna `InvoiceGeneratedEvent` olarak eklenmeli ve Debezium ile dış dünyaya fırlatılmalı.




* **Kullanılacak Teknolojiler:** Spring Scheduler, Redisson Distributed Lock, Debezium CDC



### KART 19: [PAYMENT] Idempotency-Key Kontrolü & Akıllı Ödeme Retry Zamanlayıcısı

* **Açıklama:** Ağ kesintisi veya kullanıcının butona çift basması durumunda karttan iki kez para çekilmesinin kesin olarak engellenmesi ve hata alan faturaların otomatik takibi.


* **Atanan Kişi:** Osman · Finans


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 01, KART 18 Smokin


* **Kabul Kriterleri (Checklist):**
* [ ] `payment-service` (9009) gelen `POST /api/v1/payments` isteklerinde HTTP Header alanında `Idempotency-Key` verisini zorunlu kılmalı.


* [ ] Gelen key Redis üzerine `SET NX` (24 saat TTL) ile yazılmalı. Eğer key zaten varsa ödeme POS'a atılmadan doğrudan ilk işlemin cevabı dönülmeli.


* [ ] Başarısız ödemeler için kart hata koduna göre tetiklenen bir Scheduler kurulmalı; ödeme girişimleri sırasıyla 24, 72 ve 168 saatlik periyotlarla otomatik olarak tekrar POS'a gönderilmeli.




* **Kullanılacak Teknolojiler:** Redis 7 (idempotency store), Spring Scheduler, Spring Data JPA



### KART 20: [BILLING] iText/JasperReports ile Fatura PDF Üretimi & MinIO Depolama

* **Açıklama:** Kesilen faturanın resmi PDF belgesinin üretilmesi ve güvenli şekilde bulut nesne depolama alanında saklanması.


* **Atanan Kişi:** Osman · Finans


* **Öncelik:** Yüksek


* **Bağımlılık:** KART 18


* **Kabul Kriterleri (Checklist):**
* [ ] Debezium'dan gelen `InvoiceGeneratedEvent` dinlenmeli, iText veya JasperReports kütüphanesi ile abonenin dönem borcunu, vergisini ve detayını içeren fatura PDF'i üretilmeli.


* [ ] Üretilen PDF dosyası kesinlikle lokal disk yerine S3 uyumlu `MinIO` sunucusundaki `telcox-invoices` bucket'ı altına `invoices/{customerId}/{invoiceId}.pdf` dizilimiyle yüklenmeli.


* [ ] `GET /api/v1/invoices/{id}/pdf` endpoint'i yazılmalı, çağrıldığında MinIO üzerinden sadece 10 dakika geçerli, dışa açık güvenli `presigned URL` (süreli link) döndürmeli.




* **Kullanılacak Teknolojiler:** iText 7, MinIO Java SDK, S3 Presigned URL API



### KART 21: [TICKET] Destek Talebi Yönetimi & SLA Zaman Aşımı Kontrolü

* **Açıklama:** Müşterilerin çağrı merkezine ilettiği arıza ve şikayetlerin durum takibi ve çözüm sürelerinin yasal sınır kontrolü.


* **Atanan Kişi:** Osman · İletişim & QA


* **Öncelik:** Düşük


* **Bağımlılık:** KART 01


* **Kabul Kriterleri (Checklist):**
* [ ] `ticket-service` (9010) ayağa kaldırılmalı ve destek talebi oluşturma (`POST /api/v1/tickets`) endpoint'leri yazılmalı.


* [ ] Talep oluşturulurken seçilen öncelik seviyesine (`CRITICAL`, `HIGH`, `MEDIUM`) göre sistem otomatik olarak ileri yönlü bir son çözüm tarihi (`slaDueAt`) hesaplamalı.


* [ ] Arka planda çalışan bir cron job, süresi dolan ticket'ları tarayıp outbox tablosuna `SlaBreachedEvent` kaydı atmalı; Debezium bunu okuyup Kafka üzerinden bildirim servisine iletmeli.




* **Kullanılacak Teknolojiler:** Spring Boot, Spring Data JPA, Spring Scheduler, Debezium CDC



### KART 22: [QA] Senaryo 14.2 (Aylık Fatura) ve 14.3 (Kota Aşımı) E2E Test Otomasyonu

* **Açıklama:** Fatura kesim döngüsünün ve kota aşım uyarı mekanizmalarının entegre şekilde çalıştığının test kodları ile kanıtlanması.


* **Atanan Kişi:** Osman · İletişim & QA


* **Öncelik:** Orta


* **Bağımlılık:** KART 11, KART 18, KART 20


* **Kabul Kriterleri (Checklist):**
* [ ] **14.2 Testi:** `POST /billing/runs` tetiklenmeli -> Debezium event'i ile MinIO'da PDF oluştuğu doğrulanmalı -> Ödeme endpoint'ine istek atılarak faturanın `PAID` statüsüne geçtiği assert edilmeli.


* [ ] **14.3 Testi:** CDR Simulator çalıştırılarak hedef test msisdn numarası için kota %80 ve %100 limitlerine ulaştırılmalı, Mailpit/Log katmanına doğru şablonlu SMS düştüğü doğrulanmalı.




* **Kullanılacak Teknolojiler:** RestAssured, JUnit 5, MinIO Java Client (Test doğrulama için)



---

## 📌 LİSTE: SPRINT 3 — HAFTA 3: Güçlendirme & MVP Teslim

### KART 23: [K8S] Minikube Deployment Manifestleri & K8s Secrets Entegrasyonu

* **Açıklama:** Geliştirilen tüm servislerin lokal bilgisayardaki Docker ortamından çıkarılarak Kubernetes kümesine taşınması.


* **Atanan Kişi:** Mahmut · Tech Lead


* **Öncelik:** Düşük


* **Bağımlılık:** Tüm Hafta 1 ve Hafta 2 servisleri


* **Kabul Kriterleri (Checklist):**
* [ ] Geliştirilen her mikroservis için K8s ortamına uygun `deployment.yaml` ve `service.yaml` manifest dosyaları yazılmalı.


* [ ] Veritabanında TCKN alanlarını şifrelemek için kullanılan gizli AES-GCM anahtarı kaynak kod içerisinden tamamen silinmeli ve `K8s Secrets` nesnesi olarak tanımlanıp podlara enjekte edilmeli.




* **Kullanılacak Teknolojiler:** Kubernetes, K8s Secrets, Minikube, YAML



### KART 24: [SECURITY] Yatay Ölçekleme (HPA) Konfigürasyonu & mTLS Analiz Raporu

* **Açıklama:** Sistemin anlık gelen yoğun yüklere karşı otomatik pod sayısını artırmasını sağlayacak kuralların girilmesi.


* **Atanan Kişi:** Mahmut · Tech Lead


* **Öncelik:** Düşük


* **Bağımlılık:** KART 23


* **Kabul Kriterleri (Checklist):**
* [ ] Yoğun harcama verisi işleyen `usage-service` ve fatura dönemi yüklenen `billing-service` podları için CPU tüketimi %70'i geçtiğinde çalışacak yatay ölçekleyici (`HPA`) manifesti girilmeli (Min: 1, Max: 3 pod).


* [ ] Servislerin iç ağda birbirleriyle şifreli konuşması (mTLS) mimari açıdan araştırılmalı ve teslim dökümanına MVP analiz notu olarak eklenmeli (Kodlama yapılmayacak).




* **Kullanılacak Teknolojiler:** Horizontal Pod Autoscaler (HPA), Kubernetes Core API



### KART 25: [ORDER] Ek Paket (Addon) Satın Alma & Tarife Değişikliği İş Akışları

* **Açıklama:** Kullanıcıların mevcut taahhütleri devam ederken ek internet paketi alabilmesi veya tarife yükseltme süreçlerinin iş kurallarının yazılması.


* **Atanan Kişi:** Mahmut · Core Flow


* **Öncelik:** Düşük


* **Bağımlılık:** KART 15, KART 16


* **Kabul Kriterleri (Checklist):**
* [ ] Aktif aboneliğin üzerine `POST /api/v1/orders/addons` uç noktası ile ek paket sipariş akışı eklenmeli ve orkestratör üzerinden ödeme adımı çalıştırılmalı.


* [ ] Tarife geçiş isteği geldiğinde eski kota kaydı kapatılmalı, yeni kota tanımlanmalı ve aradaki fiyat farkı bir sonraki fatura dönemine yansıtılmak üzere Billing servisine bildirilmeli.


* [ ] `SagaState` tablosundaki `lastUpdated` zaman damgasına bakarak 5 dakikadan uzun süredir askıda kalan işlemleri bulan bir cron job yazılmalı, yöneticiler için manuel `compensate` tetikleme endpoint'i açılmalı.




* **Kullanılacak Teknolojiler:** Spring Data JPA, Spring Scheduler, Saga Framework



### KART 26: [TEST] Order ↔ Payment Servisleri Arası Sözleşme (Contract) Testleri

* **Açıklama:** İki servis arasındaki event şemalarından biri değiştiğinde sistemin derleme anında patlayarak hatayı önceden yakalamasını sağlayan koruma testleri.


* **Atanan Kişi:** Mahmut · Core Flow


* **Öncelik:** Düşük


* **Bağımlılık:** KART 25


* **Kabul Kriterleri (Checklist):**
* [ ] Spring Cloud Contract kütüphanesi kullanılarak `order-service` (Consumer) ve `payment-service` (Producer) arasındaki event şemaları için sözleşme (groovy/yaml contract) tanımlanmalı.


* [ ] Ödeme servisinin ürettiği `PaymentCompleted` payload'ı değiştiğinde, sipariş servisinin testleri derleme aşamasında (build time) başarısız olmalı ve hata fırlatmalı.




* **Kullanılacak Teknolojiler:** Spring Cloud Contract, Pact Framework, JUnit 5



### KART 27: [BILLING] Kota Aşım (Overage) Ücretlendirmesi & Harcama Kalemleri

* **Açıklama:** Kotasını bitiren abonenin simülatörden gelen ekstra kullanımlarının (aşım miktarı) hesaplanarak faturaya ek satır harcaması olarak eklenmesi.


* **Atanan Kişi:** Osman · Finans


* **Öncelik:** Orta


* **Bağımlılık:** KART 09, KART 18


* **Kabul Kriterleri (Checklist):**
* [ ] `QuotaExceeded` (%100 kota aşımı) durumu oluştuktan sonra `usage-service` tarafından gelen yeni kullanım verileri `billing_extra` (aşım) flag'i ile veritabanına işaretlenmeli.


* [ ] Bill-run job çalıştığında bu harcamalar `BigDecimal` veri tipi üzerinden hesaplanmalı ve ana faturanın altına `InvoiceLine` (aşım kalemi) olarak eklenmeli (Para birimi kesinlikle `TRY` olmalı).




* **Kullanılacak Teknolojiler:** Java BigDecimal API, Spring Data JPA, PostgreSQL 16



### KART 28: [PAYMENT] Dijital Cüzdan (Wallet Balance) Sistemi & Ödeme Önceliği

* **Açıklama:** Müşterilerin sistemde bakiye tutabilmesi ve fatura ödeme anında kredi kartından önce bu bakiyenin harcanması altyapısı.


* **Atanan Kişi:** Osman · Finans


* **Öncelik:** Orta


* **Bağımlılık:** KART 19


* **Kabul Kriterleri (Checklist):**
* [ ] `payment-service` içerisine müşteri bazlı `walletBalance` alanı Flyway ile eklenmeli ve cüzdana para yükleme (`POST /api/v1/payments/wallet/top-up`) endpoint'i yazılmalı.


* [ ] Fatura ödeme tetiklendiğinde sistem önce abonenin cüzdan bakiyesini kontrol etmeli; bakiye fatura tutarına yetiyorsa doğrudan cüzdandan düşmeli, yetmiyorsa kalan tutar için sanal POS (PSP Mock) çağrısı yapmalı.




* **Kullanılacak Teknolojiler:** Spring Data JPA, Java Cryptography (Cüzdan güvenliği için), Flyway



### KART 29: [PERFORMANCE] k6/JMeter ile 1K Kullanıcı Bill-run Yük Testi

* **Açıklama:** Sistemin canlıya çıkmadan önce toplu fatura kesim esnasındaki performans limitlerinin ve hızının ölçülmesi.


* **Atanan Kişi:** Osman · Finans


* **Öncelik:** Orta


* **Bağımlılık:** KART 27


* **Kabul Kriterleri (Checklist):**
* [ ] k6 veya Apache JMeter aracı kullanılarak veritabanında hazır bulunan 1000 sanal abone verisi üzerinde toplu fatura kesim senaryosu koşturulmalı.


* [ ] Yapılan performans testinde 1000 abonenin fatura kesim, outbox yazım ve PDF üretim adımlarının tamamı **5 dakikanın altında** (p95 < 5dk) sonuçlanmalı ve test raporu proje dosyalarına eklenmeli.




* **Kullanılacak Teknolojiler:** k6 Load Testing Tool, JavaScript, Grafana (Test takibi için)



### KART 30: [GATEWAY] Merkezi Swagger UI Agregasyonu

* **Açıklama:** Geliştiricilerin ve test ekiplerinin tüm servislerin API dökümanlarına tek bir web arayüzünden erişebilmesinin sağlanması.


* **Atanan Kişi:** Mahmut · İletişim & QA


* **Öncelik:** Orta


* **Bağımlılık:** Tüm servislerin ayağa kalkmış olması gerekir.


* **Kabul Kriterleri (Checklist):**
* [ ] `api-gateway` (8080) üzerinde OpenAPI konfigurasyonu yapılmalı.


* [ ] Kullanıcı tarayıcıdan `http://localhost:8080/swagger-ui.html` adresine girdiğinde, sağ üstteki açılır menüden (`identity`, `customer`, `billing` vb.) tüm iç servislerin endpoint dokümanlarını tek ekranda görebilmeli (Swagger Agregasyonu).




* **Kullanılacak Teknolojiler:** Springdoc OpenAPI UI, Spring Cloud Gateway OpenAPI Integration



### KART 31: [QA] 3 Temel Kabul Senaryosunun Nihai Doğrulanması & Demo Raporu

* **Açıklama:** MVP teslimatı öncesinde iş birimlerine sunulacak olan 3 ana senaryonun hatasız çalıştığının kanıtlanması ve kapanış regresyon testi.


* **Atanan Kişi:** Osman · İletişim & QA


* **Öncelik:** Yüksek


* **Bağımlılık:** Projedeki tüm kartların tamamlanmış olması gerekir.


* **Kabul Kriterleri (Checklist):**
* [ ] **14.1 (Onboarding)**, **14.2 (Aylık Fatura)** ve **14.3 (Kota Aşımı)** kabul senaryoları entegrasyon test suitinde sırayla koşturulmalı ve %100 başarı (yeşil) raporu üretilmeli.


* [ ] Tüm servislerin local deployment adımlarını, docker-compose komutlarını ve gerekli .env değişkenlerini listeleyen eksiksiz bir `README.md` dökümanı hazırlanmalı.


* [ ] Postman Collection Runner aracılığıyla senaryoların arka arkaya hatasız aktığını gösteren kısa bir teknik ekran kaydı (Demo Videosu) çekilerek teslimat paketine eklenmeli ve genel regresyon koşusu başarıyla bitirilmeli.




* **Kullanılacak Teknolojiler:** RestAssured, JUnit 5, Postman Runner, Camtasia/OBS (Ekran kaydı için)