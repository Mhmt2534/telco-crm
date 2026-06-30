# Telco CRM Platform

Telco CRM Platform, bir telekomünikasyon şirketinin müşteri ilişkileri yönetimini (CRM) destekleyen mikroservis tabanlı bir backend altyapısıdır. Müşteri kaydından ürün kataloguna, sipariş yönetiminden faturalamaya kadar tüm operasyonlar bağımsız, ayrı servisler olarak çalışır. Her servis kendi veritabanına sahiptir, birbirleriyle REST (OpenFeign) veya Kafka üzerinden haberleşir.

Projede paylaşılan altyapı kodu — hata yönetimi, loglama, veritabanı audit, Kafka outbox gibi mekanizmalar — üç ortak modülde (`common-core`, `common-web`, `common-persistence`) toplanmıştır. Yeni bir servis geliştirirken bu modülleri dependency olarak ekleyerek sıfırdan kod yazmak zorunda kalmazsın.

---

## İçindekiler

- [Proje Yapısı](#proje-yapısı)
- [Ortak Modüller](#ortak-modüller)
- [Gereksinimler](#gereksinimler)
- [Nasıl Çalıştırılır](#nasıl-çalıştırılır)
- [Ortak Modüller Nasıl Kullanılır](#ortak-modüller-nasıl-kullanılır)
- [Yeni Servis Geliştirme Akışı](#yeni-servis-geliştirme-akışı)
- [Servis Portları](#servis-portları)
- [Geliştirici Araçları](#geliştirici-araçları)

---

## Proje Yapısı

```
telco-crm-microservices/
├── pom.xml                    ← Root POM, tüm versiyonlar burada yönetilir
├── docker-compose.yml         ← PostgreSQL, Kafka, Redis, Zipkin
├── config-repo/               ← Merkezi konfigürasyon dosyaları (git-backed)
│
├── common-core/               ← Paylaşılan saf kontratlar (event, exception, model)
├── common-web/                ← Servlet-stack paylaşılan katman (exception handler, filtre)
├── common-persistence/        ← JPA paylaşılan katman (BaseEntity, Outbox, Idempotency)
│
├── discovery-server/          ← Eureka servis kaydı          :8761
├── config-server/             ← Merkezi konfigürasyon sunucusu :8888
├── api-gateway/               ← Edge routing + JWT doğrulama   :8080
│
├── identity-service/          ← Auth, JWT, kullanıcı yönetimi  :9001
├── customer-service/          ← Müşteri, KYC, adres            :9002
├── product-catalog-service/   ← Tarife, ek paket, teklifler    :9003
├── order-service/             ← Sipariş ve saga orkestrasyon   :9004
├── subscription-service/      ← Abonelik, MSISDN, SIM kart     :9005
├── usage-service/             ← Kullanım kaydı, kota           :9006
├── billing-service/           ← Fatura, fatura döngüsü         :9007
├── payment-service/           ← Ödeme, cüzdan                  :9008
├── notification-service/      ← Bildirim, şablon, kanal        :9009
└── ticket-service/            ← Destek talebi, SLA             :9010
```

---

## Ortak Modüller

### `common-core`

**Ne yapar?** Projenin tüm servisleri tarafından kullanılabilen en temel paylaşılan sözleşmeleri barındırır. Spring MVC veya JPA gibi ağır bağımlılıkları yoktur; bu sayede hem servlet tabanlı servisler hem de reaktif `api-gateway` tarafından tüketilebilir.

**İçerdiği yapılar:**


| Paket / Sınıf                                                                      | Açıklama                                                             |
| ---------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| `EventConstants`                                                                   | Tüm Kafka topic isimlerini sabit olarak tanımlar                     |
| `HeaderConstants`                                                                  | `X-User-Id`, `X-Correlation-Id` gibi gateway header isimlerini tutar |
| `BaseDomainEventEnvelope`                                                          | Her Kafka event'inin sarmalandığı standart zarf                      |
| `UserContext`                                                                      | Gateway'den gelen `X-User-Id` ve `X-User-Roles` bilgilerini taşır    |
| `BaseBusinessException`, `ResourceNotFoundException`, `DuplicateResourceException` | Tüm servislerde kullanılacak temel exception sınıfları               |
| `ErrorCode`                                                                        | Standart hata kodları enum'u                                         |
| `ProblemDetails`                                                                   | RFC 7807 uyumlu hata yanıt modeli                                    |
| `MoneyValueObject`                                                                 | Para birimi ve tutar için value object                               |
| `PageResponse<T>`                                                                  | Sayfalı liste yanıtları için standart model                          |
| `logback-spring.xml`                                                               | Tüm servislerin JSON formatında log üretmesini sağlar                |


**Neden kullanırsın?** Hata fırlattığında `ResourceNotFoundException` kullanırsan tüm servisler aynı formatta hata döner. Kafka event'i tanımlarken `EventConstants.ORDER_CREATED` gibi sabitlerden faydalanırsın; magic string yazmazsın.

---

### `common-web`

**Ne yapar?** Servlet tabanlı (Spring MVC) servislerin HTTP katmanına ait ortak altyapıyı sağlar. Bunu ekleyen her servis otomatik olarak şu özelliklere kavuşur:


| Bileşen                                | Açıklama                                                                                                                                                                        |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GlobalExceptionHandler`               | `BaseBusinessException` alt sınıflarını yakalar, RFC 7807 formatlı `ProblemDetails` döndürür                                                                                    |
| `CorrelationIdFilter`                  | Gelen istekteki `X-Correlation-Id` header'ını alır, MDC'ye koyar; böylece her log satırına correlation ID otomatik eklenir                                                      |
| `UserContextResolver` + `@CurrentUser` | Controller metodlarında `@CurrentUser UserContext user` parametresi kullanmaya olanak tanır; gateway'in eklediği `X-User-Id` / `X-User-Roles` header'larını otomatik parse eder |
| `PageableResponseHelper`               | Spring Data `Page<T>` nesnesini standart `PageResponse<T>` formatına çevirir                                                                                                    |


**Neden kullanırsın?** Gelen istekte kim olduğunu her serviste elle parse etmek zorunda kalmazsın. Exception handling, loglama ve pagination standartlarını ücretsiz alırsın.

---

### `common-persistence`

**Ne yapar?** JPA kullanan servislerin veri katmanı için temel yapı taşlarını sağlar.


| Bileşen                                                | Açıklama                                                                                                                                                                                     |
| ------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `BaseEntity`                                           | `id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy` alanlarını otomatik yönetir; tüm entity sınıfları bunu extend eder                                                                  |
| `OutboxEvent` + `OutboxRepository` + `OutboxPublisher` | Transactional Outbox pattern implementasyonu. Kafka'ya event göndermek için doğrudan `KafkaTemplate` kullanmak yerine outbox tablosuna yazılır; publisher worker bunu okuyup Kafka'ya iletir |
| `IdempotentConsumer` + `ProcessedEvent`                | Kafka consumer'larının aynı event'i iki kez işlemesini önler. Event ID veritabanına kaydedilir; duplicate gelirse işlem atlanır                                                              |
| `PiiEncryptionConverter`                               | TCKN gibi kişisel verileri veritabanında şifreli saklamak için JPA `AttributeConverter`                                                                                                      |
| SQL Şablonları                                         | `base_entity_columns.sql`, `outbox_event.sql`, `processed_event.sql` migration şablonları                                                                                                    |


**Neden kullanırsın?** Her entity için elle `createdAt` / `updatedAt` yönetmek zorunda kalmazsın. Kafka'ya event gönderirken veri kaybı riskini Outbox pattern ile ortadan kaldırsın diye kullanırsın.

---

## Gereksinimler


| Araç           | Minimum Versiyon                              |
| -------------- | --------------------------------------------- |
| Java           | **21**                                        |
| Maven          | **3.9+** (repoda Maven Wrapper `./mvnw` gömülü — kurulu Maven şart değil) |
| Docker         | **24+**                                       |
| Docker Compose | **2.x** (Compose V2, `docker compose` komutu) |


---

## Nasıl Çalıştırılır

### 1. Altyapıyı başlat

PostgreSQL, Kafka, Redis ve Zipkin container'larını ayağa kaldır:

```bash
cd telco-crm-microservices
docker compose up -d
```

Servislerin sağlıklı olduğunu doğrulamak için:

```bash
docker compose ps
```

Tüm servisler `healthy` olana kadar bekle (yaklaşık 30-60 saniye).

### 2. Tüm modülleri derle

Proje, sabit Maven sürümünü (`3.9.10`) garanti eden bir **Maven Wrapper** içerir. Kurulu bir Maven'a ihtiyaç duymadan wrapper'ı kullan:

```bash
# Linux / macOS
./mvnw clean install

# Windows (PowerShell / CMD)
.\mvnw.cmd clean install
```

İlk kurulumda build süresini kısaltmak için testleri atlayabilirsin:

```bash
./mvnw clean install -DskipTests
```

Tek bir servisi wrapper ile çalıştırmak için:

```bash
./mvnw -pl identity-service spring-boot:run
```

> Kurulu Maven kullanmak istersen `./mvnw` yerine `mvn` yazabilirsin; davranış aynıdır.
> `-DskipTests` flag'i ilk kurulumda build süresini kısaltır. Test koşmak istersen çıkart.

### 3. Config Server ve Discovery Server'ı başlat

Diğer servisler bunlara bağlandığı için önce bu ikisinin ayakta olması gerekir:

```bash
# Ayrı terminallerde çalıştır
java -jar config-server/target/config-server-1.0.0-SNAPSHOT.jar
java -jar discovery-server/target/discovery-server-1.0.0-SNAPSHOT.jar
```

### 4. Servisleri başlat

```bash
java -jar identity-service/target/identity-service-1.0.0-SNAPSHOT.jar
java -jar customer-service/target/customer-service-1.0.0-SNAPSHOT.jar
# ... diğer servisler
```

> Alternatif olarak PowerShell start scripti kullanabilirsin:
>
> ```powershell
> .\start-platform.ps1
> ```

### 5. Doğrulama


| URL                                                                            | Açıklama                              |
| ------------------------------------------------------------------------------ | ------------------------------------- |
| [http://localhost:8761](http://localhost:8761)                                 | Eureka — kayıtlı servisleri görüntüle |
| [http://localhost:8888/actuator/health](http://localhost:8888/actuator/health) | Config Server sağlık durumu           |
| [http://localhost:9411](http://localhost:9411)                                 | Zipkin — dağıtık tracing              |
| [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | API Gateway sağlık durumu             |


---

## Ortak Modüller Nasıl Kullanılır

Versiyon bilgisini root `pom.xml` yönettiği için modül POM'larında versiyon yazmana gerek yok.

### `common-core` — Her modül için (gateway dahil)

```xml
<dependency>
    <groupId>com.turkcell.springmicroservices</groupId>
    <artifactId>common-core</artifactId>
</dependency>
```

**Ne zaman eklersin?** Servisin exception fırlatacaksa, Kafka event gönderecekse veya `UserContext`'e erişecekse.

```java
// Exception fırlatma — GlobalExceptionHandler otomatik yakalar, RFC 7807 döner
throw new ResourceNotFoundException("Customer", customerId);

// Kafka event'i için sabit kullan
kafkaTemplate.send(EventConstants.ORDER_CREATED, event);
```

---

### `common-web` — Servlet tabanlı servisler için

```xml
<dependency>
    <groupId>com.turkcell.springmicroservices</groupId>
    <artifactId>common-web</artifactId>
</dependency>
```

> `api-gateway`'e ekleme — gateway reaktif stack (WebFlux) kullanır, `common-web` bunu bozar.

**Ne zaman eklersin?** Servisin HTTP endpoint'leri varsa (yani Spring MVC kullanıyorsa).

```java
// Controller'da giriş yapan kullanıcıya erişim
@GetMapping("/profile")
public ResponseEntity<ProfileDto> getProfile(@CurrentUser UserContext user) {
    return ResponseEntity.ok(customerService.getProfile(user.getUserId()));
}
```

Loglarda correlation ID otomatik görünür — elle bir şey yapman gerekmez.

---

### `common-persistence` — JPA kullanan servisler için

```xml
<dependency>
    <groupId>com.turkcell.springmicroservices</groupId>
    <artifactId>common-persistence</artifactId>
</dependency>
```

**Ne zaman eklersin?** Servisin veritabanı entity'leri varsa ve/veya Kafka event yayınlıyorsa.

```java
// Entity tanımı — createdAt, updatedAt, id otomatik yönetilir
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {
    private String name;

    // PII alanlarını şifreli sakla
    @Convert(converter = PiiEncryptionConverter.class)
    private String nationalId; // TCKN veritabanında şifreli tutulur
}
```

```java
// Outbox pattern ile Kafka event gönderme
// Hem domain değişikliği hem outbox kaydı TEK transaction içinde yazılır
@Transactional
public void createCustomer(CreateCustomerRequest request) {
    Customer customer = customerRepository.save(new Customer(request));
    outboxPublisher.publish(EventConstants.CUSTOMER_REGISTERED, customer.getId(), payload);
}
```

```java
// Idempotent Kafka consumer — aynı event iki kez işlenmez
@KafkaListener(topics = EventConstants.ORDER_CREATED)
public void onOrderCreated(BaseDomainEventEnvelope<OrderCreatedPayload> event) {
    idempotentConsumer.process(event.getEventId(), () -> {
        // Bu blok her event ID için yalnızca bir kez çalışır
        subscriptionService.activate(event.getPayload());
    });
}
```

---

## Yeni Servis Geliştirme Akışı

1. **Root `pom.xml`'e modül ekle**
  `<modules>` bloğuna yeni servisin adını ekle:
2. **Modül klasörünü ve `pom.xml`'ini oluştur**
  Parent olarak root POM'u göster; versiyon yazmana gerek yok:
3. **Ortak dependency'leri ekle**
  Servlet tabanlı bir servis için standart set:
4. **Veritabanını Docker Compose'a ekle**
  Her servis kendi izole PostgreSQL container'ına sahiptir (Database Per Service). `docker-compose.yml` dosyasına yeni bir servis bloğu ekle:
   `volumes:` bölümüne `my-new-service-db-data:` satırını da ekle.
5. `**application.yml` dosyasını `config-repo/` altına ekle**
  `config-repo/my-new-service.yml` oluştur ve port, datasource, Kafka bağlantılarını tanımla.
6. **Entity, Repository, Service, Controller katmanlarını oluştur**
  - Entity → `BaseEntity`'yi extend et
  - Repository → `JpaRepository` kullan
  - Service → `@Transactional` ve Outbox pattern
  - Controller → `@CurrentUser` ile kullanıcı bağlamına eriş
7. **Servisi Eureka'ya kaydet**
  `application.yml`'e şunu ekle:
8. **Build al ve çalıştır**
  ```bash
   mvn clean install -pl my-new-service -am -DskipTests
   java -jar my-new-service/target/my-new-service-1.0.0-SNAPSHOT.jar
  ```

---

## Servis Portları


| Servis                    | Port |
| ------------------------- | ---- |
| API Gateway               | 8080 |
| Discovery Server (Eureka) | 8761 |
| Config Server             | 8888 |
| Identity Service          | 9001 |
| Customer Service          | 9002 |
| Product Catalog Service   | 9003 |
| Order Service             | 9004 |
| Subscription Service      | 9005 |
| Usage Service             | 9006 |
| Billing Service           | 9007 |
| Payment Service           | 9008 |
| Notification Service      | 9009 |
| Ticket Service            | 9010 |


**Altyapı bileşenleri (Docker — host portları):**


| Bileşen         | Host Port | Açıklama                                                         |
| --------------- | --------- | ---------------------------------------------------------------- |
| identity-db     | 5433      | `identity_db` — identity-service'e özel PostgreSQL               |
| customer-db     | 5434      | `customer_db` — customer-service'e özel PostgreSQL               |
| product-db      | 5435      | `product_catalog_db` — product-catalog-service'e özel PostgreSQL |
| order-db        | 5436      | `order_db` — order-service'e özel PostgreSQL                     |
| subscription-db | 5437      | `subscription_db` — subscription-service'e özel PostgreSQL       |
| billing-db      | 5438      | `billing_db` — billing-service'e özel PostgreSQL                 |
| usage-db        | 5439      | `usage_db` — usage-service'e özel PostgreSQL                     |
| notification-db | 5440      | `notification_db` — notification-service'e özel PostgreSQL       |
| ticket-db       | 5441      | `ticket_db` — ticket-service'e özel PostgreSQL                   |
| payment-db      | 5442      | `payment_db` — payment-service'e özel PostgreSQL                 |
| Kafka           | 9092      | Mesaj broker (KRaft modu, Zookeeper yok)                         |
| Redis           | 6379      | Cache ve idempotency store                                       |
| Zipkin          | 9411      | Dağıtık tracing UI                                               |


---

## Geliştirici Araçları

Opsiyonel araçlar `tools` profili ile başlatılır — normal `docker compose up -d` ile başlamaz:

```bash
docker compose --profile tools up -d
```


| Araç         | URL                                            | Açıklama                             |
| ------------ | ---------------------------------------------- | ------------------------------------ |
| Kafka UI     | [http://localhost:8085](http://localhost:8085) | Kafka topic ve mesajlarını görüntüle |
| Mailpit      | [http://localhost:8025](http://localhost:8025) | Mock e-posta sunucusu (SMTP: 1025)   |
| RedisInsight | [http://localhost:5540](http://localhost:5540) | Redis içeriğini görüntüle            |


> RedisInsight'a bağlanmak için manuel olarak `host: redis`, `port: 6379` bağlantısı eklemen gerekir.

---

# Ekstra not

## Docker ayağa kalkmadan önce terminal yazılacaklar:

```bash
docker pull maven:3.9-eclipse-temurin-21

docker pull eclipse-temurin:21-jre

docker compose build config-server

docker compose up -d --build
```

## Docker Profiller

```bash
# Sadece altyapı
docker compose up -d
# Altyapı + DB'ler
docker compose --profile dbs up -d
# Tam stack
docker compose --profile dbs --profile apps up -d
# Geliştirici araçları dahil tam stack
docker compose --profile dbs --profile apps --profile tools up -d
# Eğer sadece bir veya birden fazla servisi ayağa kaldırmak istersek
docker compose up -d customer-service
```

