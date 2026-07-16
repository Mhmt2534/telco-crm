# Servisler Arası ID Tutarlılığı — ADIM 1 Envanter Raporu

Tarih: 2026-07-16  
Kapsam: `identity-service`, `customer-service`, `product-catalog-service`, `order-service`, `subscription-service`, `usage-service`, `billing-service`, `payment-service`, `notification-service`, `ticket-service`

## Amaç ve yöntem

Bu rapor yalnızca mevcut durumu belgeler. Uygulama kodu, DTO, entity, repository, Feign istemcisi veya Flyway migration değiştirilmemiştir.

İnceleme; JPA entity/repository tanımları, Flyway SQL'leri, REST controller ve response/request DTO'ları, MapStruct mapper'ları, OpenFeign arayüzleri, outbox payload üreticileri ve Kafka consumer DTO'ları birlikte izlenerek yapılmıştır. `BaseEntity` kullanan entity'lerin PK tipi, `common-persistence` içindeki `BaseEntity.id` alanından (UUID) doğrulanmıştır.

Terimler:

- **Internal**: Servisin kendi veritabanı PK/FK değeri.
- **Public**: Servis sınırı dışına REST, Feign veya event ile çıkan kimlik.
- **Doğal anahtar**: UUID yerine servis sınırında kullanılan `code`, `msisdn`, `iccid` gibi iş anahtarı.
- Outbox tablosunun teknik `id` alanı, event payload kimliği sayılmamıştır; `aggregate_id` ve JSON payload ayrıca incelenmiştir.

## Yönetici özeti

| Servis | Ana aggregate PK | Ayrı `public_id` var mı? | Dışarı sızan internal ID | Başlıca uyumsuzluk |
|---|---|---:|---|---|
| identity-service | `OtpAttempt: Long` | Hayır | Yok | Müşteri kimliği taşımıyor; yalnız Keycloak user ID `String` kullanıyor |
| customer-service | `Customer: Long` | Hayır | REST `id`, path `id`, 3 customer eventi | Order/billing Long bekliyor; ticket/notification UUID varsayıyor |
| product-catalog-service | `Tariff/Addon: Long` | Hayır | Response `id` | Servisler arası çağrılar çoğunlukla `code: String`; eventlerde DB ID yok |
| order-service | `Order/OrderItem: Long` | Hayır | REST, Feign customer ref, order eventleri | `orderId/customerId` Long; downstream event zinciri buna bağlı |
| subscription-service | `Subscription: UUID` | Gerekmez | `customerId/orderId: Long` foreign ref | Kendi ID'si doğru UUID; customer/order referansları internal Long |
| usage-service | `Quota/UsageRecord: UUID` | Gerekmez | `orderId/customerId: Long` yalnız tariff-change consumer DTO'sunda | Subscription ref UUID ile tutarlı |
| billing-service | `BillCycle/Invoice/InvoiceLine: Long` | Hayır | Invoice path/eventleri, customer ref | Notification UUID beklerken producer Long üretiyor |
| payment-service | `Payment/Attempt/Wallet: UUID` | Gerekmez | `orderId: Long`, `invoiceId/customerId: String` | Aynı `PaymentCompleted` için iki farklı payload şekli var |
| notification-service | Tümü UUID | Gerekmez | Kendi REST ID'si UUID | Consumer DTO'ları upstream Long customer/invoice kimlikleriyle uyumsuz |
| ticket-service | `Ticket: UUID` | Gerekmez | Kendi ID'si UUID | `customerId: UUID`, mevcut customer-service public ID sunmuyor |

Depoda incelenen 10 serviste hiçbir `public_id` / `publicId` alanı bulunmamaktadır.

## 1. Entity ve primary key envanteri

### Domain ve destek entity'leri

| Servis | Entity / tablo | Java PK | SQL PK | Not |
|---|---|---:|---|---|
| identity-service | `OtpAttempt` / `otp_attempt` | `Long` | `BIGSERIAL` | REST response/path içinde kullanılmıyor |
| customer-service | `Customer` / `customer` | `Long` | `BIGSERIAL` | Ana aggregate; dışarı sızıyor |
| customer-service | `Address` / `address` | `Long` | `BIGSERIAL` | `customer_id BIGINT` ile internal join; `AddressDto` ID döndürmüyor |
| customer-service | `Document` / `document` | `Long` | `BIGSERIAL` | `customer_id BIGINT` ile internal join; response'a çıkmıyor |
| product-catalog-service | `Tariff` / `tariff` | `Long` | `BIGSERIAL` | Response `id` internal PK; path'lerde `code` kullanılıyor |
| product-catalog-service | `Addon` / `addon` | `Long` | `BIGSERIAL` | Response `id` internal PK; path'lerde `code` kullanılıyor |
| product-catalog-service | `TariffAddon` / `tariff_addon` | `TariffAddonId(Long tariffId, Long addonId)` | Bileşik `BIGINT` PK | Tamamen internal join |
| order-service | `Order` / `orders` | `Long` | `BIGSERIAL` | Ana aggregate; REST ve eventlerde dışarı sızıyor |
| order-service | `OrderItem` / `order_item` | `Long` | `BIGSERIAL` | `OrderItemResponse.id` ile dışarı sızıyor; `order_id BIGINT` internal join |
| order-service | `SagaState` / `saga_state` | `Long` | `BIGSERIAL` | Teknik entity; `orderId Long` internal bağ; `sagaId String(UUID)` ayrı iş kimliği |
| subscription-service | `Subscription` / `subscriptions` | `UUID` (`BaseEntity`) | `UUID` | Kendi ID'si hem internal hem public rolünde |
| subscription-service | `SubscriptionAddon` / `subscription_addons` | `UUID` | `UUID` | `subscription_id UUID` internal FK |
| subscription-service | `MsisdnPool` / `msisdn_pool` | `String msisdn` | `VARCHAR` | Doğal anahtar |
| subscription-service | `SimCard` / `sim_cards` | `String iccid` | `VARCHAR` | Doğal anahtar |
| usage-service | `Quota` / `quotas` | `UUID` (`BaseEntity`) | `UUID` | `subscription_id UUID` external ref |
| usage-service | `UsageRecord` / `usage_records` | `UUID` (`BaseEntity`) | `UUID` | `subscription_id UUID` external ref |
| billing-service | `BillCycle` / `bill_cycle` | `Long` | `BIGSERIAL` | `customerId Long`, `subscriptionId UUID` |
| billing-service | `Invoice` / `invoice` | `Long` | `BIGSERIAL` | Ana aggregate; REST/eventlerde dışarı sızıyor |
| billing-service | `InvoiceLine` / `invoice_line` | `Long` | `BIGSERIAL` | `invoice_id BIGINT` internal join; dış response yok |
| payment-service | `Payment` / `payments` | `UUID` (`BaseEntity`) | `UUID` | Kendi ID'si hem internal hem public rolünde |
| payment-service | `PaymentAttempt` / `payment_attempts` | `UUID` (`BaseEntity`) | `UUID` | `payment_id UUID` internal FK |
| payment-service | `Wallet` / `wallets` | `UUID` (`BaseEntity`) | `UUID` | Dış response wallet ID'sini döndürmüyor |
| notification-service | `NotificationTemplate` | `UUID` | `UUID` | REST response/delete path UUID |
| notification-service | `NotificationHistory` | `UUID` | `UUID` | REST'e çıkmıyor |
| notification-service | `CommunicationPreference` | `UUID userId` | `UUID` | Customer/user referansı aynı zamanda PK |
| ticket-service | `Ticket` / `tickets` | `UUID` | `UUID` | Kendi ID'si hem internal hem public rolünde |

### Outbox teknik entity'leri

| Servis | Outbox PK | `aggregate_id` tipi | Durum |
|---|---:|---:|---|
| customer-service | `Long / BIGSERIAL` | `String` | Customer internal Long ID'sinin string hali yazılıyor |
| product-catalog-service | `Long / BIGSERIAL` | `String` | Tariff `code` yazılıyor; internal PK yazılmıyor |
| order-service | `Long / BIGSERIAL` | `String` | Çoğunlukla Order internal Long ID'sinin string hali |
| subscription-service | `UUID` | `String` | Subscription UUID; hata eventinde order Long string olabilir |
| usage-service | `UUID` | `String` | Subscription UUID |
| billing-service | `UUID` | `String` | Invoice internal Long ID'sinin string hali |
| payment-service | `UUID` | `String` | Payment UUID |
| notification-service | `UUID` | `String` | Entity mevcut fakat incelenen notification akışı outbox yazmıyor |
| ticket-service | `UUID` | `String` | Ticket UUID |
| identity-service | Outbox yok | — | Event üretmiyor |

## 2. REST, DTO, path ve Feign envanteri

| Servis | REST response'taki kimlikler | Path/query/request kimlikleri | Servisler arası referans ve Feign tipi | Değerlendirme |
|---|---|---|---|---|
| identity-service | Auth response'larında domain ID yok; token içindeki Keycloak alanları `String` | Telefon ve token alanları `String` | `CustomerServiceClient` telefonla çağırır; `CustomerOtpInfo.keycloakUserId: String`, customer DB ID yok | `OtpAttempt.id Long` dışarı çıkmıyor |
| customer-service | `CustomerResponse.id: Long`, mapper doğrudan `Customer.id` map eder | `GET/PUT/DELETE /customers/{id}` ve KYC approve path'i `Long` | Identity için internal endpoint telefonla çalışır ve DB ID döndürmez | Ana internal PK doğrudan public API'de |
| product-catalog-service | `TariffResponse.id: Long`, `AddonResponse.id: Long`; mapper doğrudan entity ID map eder. `ProductResponse` yalnız `productCode: String` | Tariff/addon CRUD path'leri `code: String`; batch/list filtreleri code kullanır | Order ve usage Feign çağrıları product/tariff `code: String` kullanır | Response ID sızıntısı var; inter-service akış doğal anahtarla daha tutarlı |
| order-service | `OrderResponse.id: Long`, `OrderItemResponse.id: Long`, `customerId: Long`; mapper internal ID'leri map eder | Order `{id}: Long`; liste `customerId: Long`; create/addon/tariff-change request `customerId: Long`; `subscriptionId: UUID` | Customer Feign path/DTO `Long`; product `code: String`; subscription path/DTO `UUID` fakat DTO içindeki `customerId: Long` | En yoğun internal ID sızıntısı ve bağımlılık merkezi |
| subscription-service | `SubscriptionResponse.id: UUID`, `customerId: Long`; kendi ID'si doğru | Tüm subscription `{id}` path'leri `UUID`; create request `customerId: Long`, `orderId: Long`, `tariffCode: String` | Order eventlerinden `orderId/customerId Long`, tariff/subscription doğal/UUID ref alır | Kendi PK'sına migration gerekmez; foreign ref'ler değişmeli |
| usage-service | `QuotaResponse.subscriptionId: UUID`; quota entity ID'sini response'a koymuyor | Subscription quota/overage path'i `UUID` | ProductCatalog Feign `tariff code: String`; CDR ve quota akışı `subscriptionId: UUID`; tariff-change DTO ayrıca `orderId/customerId: Long` taşır | Subscription ref tutarlı; order/customer event alanları tutarsız |
| billing-service | Invoice entity response DTO'su yok; PDF endpoint'i `Map<String,String>` döner | `GET /invoices/{id}/pdf` path'i `Long` | Customer Feign path/DTO `Long`; Usage Feign subscription `UUID`; entitylerde `customerId Long`, `subscriptionId UUID` | Invoice internal ID path'te; customer internal ID servisler arası |
| payment-service | `PaymentResponse.paymentId: UUID`; `invoiceId/customerId: String`; wallet response `customerId: String` | `PaymentRequest.invoiceId: String`, `orderId: Long`; wallet path `customerId: String`; `X-User-Id: String` customer olarak saklanıyor | Feign yok; order/payment Kafka akışı `orderId Long`; billing invoice ref sayısal String | Payment UUID doğru; `customerId` semantiği belirsiz (Customer public ID mi Keycloak subject mi) |
| notification-service | `NotificationTemplateResponse.id: UUID` | Template delete `{id}: UUID`; get path `code: String` | Feign yok; consumer DTO'ları customer ve invoice için UUID varsayıyor | Kendi ID'leri tutarlı, upstream kontratlarıyla uyumsuz |
| ticket-service | `TicketResponse.id/customerId: UUID` | `TicketCreateRequest.customerId: UUID`; yalnız create endpoint'i var | Feign/Kafka consumer yok | Ticket UUID doğru; customer-service henüz UUID public ID sağlamadığı için customer ref fiilen uyumsuz |

## 3. Outbox/Kafka event ID kontratları

Tablodaki tipler JSON'a yazılmadan önceki Java/semantik tiplerdir. `String(UUID)` ifadesi UUID'nin elle stringe çevrildiğini gösterir.

| Event | Producer ve ürettiği ID alanları | Consumer ve beklediği ID alanları | Mevcut durum |
|---|---|---|---|
| `CustomerRegistered` | customer: `customerId Long`; aggregate ID `String(Long)` | Depoda consumer bulunamadı | Internal customer PK dışarı çıkıyor |
| `CustomerUpdated` | customer: `customerId Long`; `keycloakUserId String`; aggregate ID `String(Long)` | Depoda consumer bulunamadı | Internal customer PK dışarı çıkıyor |
| `CustomerKYCApproved` | customer: `customerId Long`; `keycloakUserId String`; aggregate ID `String(Long)` | Depoda consumer bulunamadı | Internal customer PK dışarı çıkıyor |
| `TariffCreated` | catalog: DB ID yok; `code String`; aggregate ID `code` | Depoda consumer bulunamadı | Kimlik doğal anahtarla tutarlı |
| `TariffPriceChanged` | catalog: DB ID yok; `code String`; aggregate ID `code` | Depoda consumer bulunamadı | Kimlik doğal anahtarla tutarlı |
| `OrderCreated` | order: `orderId Long`, `customerId Long`, opsiyonel `subscriptionId String(UUID)` | payment: `orderId Long`, `customerId String` | Customer değeri JSON number → `String` coercion'a dayanıyor; iki internal PK sızıyor |
| `OrderConfirmed` | order: `orderId Long`, `customerId Long`, opsiyonel `subscriptionId String(UUID)`, `tariffCode String` | subscription: `orderId Long`, `customerId Long`, `subscriptionId UUID`; product type/code | Producer/consumer tipleri mevcut haliyle eşleşiyor, fakat Long değerler internal |
| `TariffChangeRequested` | order: `orderId Long`, `customerId Long`, `subscriptionId String(UUID)` | subscription ve usage: `orderId Long`, `customerId Long`, `subscriptionId UUID` | Jackson string→UUID dönüşümüne dayanıyor; Long değerler internal |
| `PaymentRefundRequested` | order: `orderId Long`, `paymentId String(UUID)` | payment: `orderId Long`, `paymentId UUID` | String→UUID dönüşümüne dayanıyor; order internal PK |
| `PaymentCompleted` (order ödemesi) | payment: `paymentId UUID`, `orderId Long`, `customerId String` | order: `paymentId UUID`, `orderId Long`; billing aynı topicte aynı event adını `paymentId UUID`, `orderId Long`, `invoiceId String`, `customerId Long` olarak modeller | Order consumer eşleşiyor; billing alan eksikse event'i atlıyor; customer tipi farklı |
| `PaymentCompleted` (invoice REST ödemesi) | payment: `paymentId UUID`, `invoiceId String`, order/customer alanı yok; aggregate ID Payment UUID | billing: `invoiceId String` alıp `Long.parseLong` ile internal Invoice PK'ya çevirir; order consumer `orderId` yoksa atlar | Invoice internal Long ID sayısal String olarak sınırı geçiyor; aynı event adı iki payload şekline sahip |
| `PaymentFailed` | payment: `orderId Long`, `customerId String` | order: `orderId Long` | Order internal PK dışarı çıkıyor |
| `PaymentRefunded` | payment: `paymentId UUID`, `orderId Long` | order: `paymentId UUID`, `orderId Long` | Payment ID tutarlı, order internal PK dışarı çıkıyor |
| `SubscriptionActivated` | subscription: `orderId Long`, `subscriptionId UUID`, `msisdn String`, `status String`; aggregate ID Subscription UUID | order: `orderId Long`; billing: `subscriptionId UUID`, `customerId Long`; notification: `subscriptionId UUID`, `customerId UUID` | Producer `customerId` üretmiyor. Billing/notification DTO'larının customer beklentisi birbiriyle de farklı |
| `SubscriptionActivationFailed` | subscription: `orderId Long`; hata halinde aggregate ID `String(Long)` | order: `orderId Long` | Tipler eşleşiyor fakat internal order PK sızıyor |
| `AddonActivated` | subscription: `orderId Long`, `subscriptionId String(UUID)`, `addonCode String` | order bunu `SubscriptionActivatedPayload` ile okur: `orderId Long` esas alınır | Internal order PK sızıyor |
| `MSISDNAllocated` | subscription: tüm `Subscription` entity'sini serialize eder: entity `id UUID`, `customerId Long`, diğer audit/internal alanlar dahil olabilir | Depoda consumer bulunamadı | Entity'nin doğrudan serialize edilmesi kontrat ve internal alan sızıntısı riski |
| `CdrRecorded` | `cdr-simulator` (10 servis kapsamı dışında): `subscriptionId UUID` | usage: `subscriptionId UUID` | ID tipi tutarlı. Simulator doğrudan Kafka producer kullanır; outbox servisi değildir |
| `QuotaThresholdReached` | usage: `subscriptionId UUID`; aggregate ID Subscription UUID | notification: `subscriptionId UUID` | Tutarlı |
| `QuotaExceeded` | usage: `subscriptionId UUID`; aggregate ID Subscription UUID | notification: `subscriptionId UUID` | Tutarlı |
| `InvoiceGeneratedEvent` | billing: `invoiceId Long`, `customerId Long`, `subscriptionId UUID`; aggregate ID `String(Long)` | billing PDF consumer ham payload işler; notification DTO `invoiceId UUID`, `customerId UUID`, `subscriptionId UUID` bekler | Notification ile deserialization tipi uyumsuz; ayrıca producer/consumer event adı kontrolü farklı görünüyor (`...Event` / eventsiz) |
| `InvoiceOverdueEvent` | billing: `invoiceId Long`, `customerId Long`, `subscriptionId UUID`; aggregate ID `String(Long)` | notification DTO `invoiceId UUID`, `customerId UUID` bekler | Notification ile deserialization tipi uyumsuz; event adı kontrolünde aynı ek farkı var |
| `InvoicePaidEvent` | billing: `invoiceId Long`, `customerId Long`, `subscriptionId UUID`; aggregate ID `String(Long)` | Depoda consumer bulunamadı | Invoice/customer internal PK sızıyor |
| `TicketOpened` | ticket: `ticketId String(UUID)`, `customerId String(UUID)`; aggregate ID Ticket UUID | Depoda consumer bulunamadı | Semantik olarak UUID, fakat payload alanları String serialize ediliyor |
| `SlaBreachedEvent` | ticket: `ticketId String(UUID)`, `customerId String(UUID)`; aggregate ID Ticket UUID | Depoda consumer bulunamadı | Semantik olarak UUID, fakat payload alanları String serialize ediliyor |

## 4. Onay sonrası değişiklik için bağımlılık haritası

| Önce değişmesi gereken sahip servis | Public ID'yi kullanan/etkilenen servisler | Birlikte güncellenecek sınırlar |
|---|---|---|
| identity-service | Şu an yok | `OtpAttempt` ID'si dışarı çıkmadığı için public ID gereksiniminin literal kapsamı netleştirilmeli |
| customer-service | order, subscription, billing, payment, notification, ticket | Customer REST/Feign, order request/entity/eventleri, subscription/billing refs, payment customer semantiği, ticket request/entity, notification DTO'ları |
| product-catalog-service | order, subscription, usage | REST response ID'leri; inter-service akış zaten `code` kullandığı için `code` ile public UUID rolü çakıştırılmamalı |
| order-service | payment, subscription, usage, billing | Order REST/path; saga internal lookup; tüm order/payment/subscription event producer+consumer çiftleri |
| subscription-service | order, usage, billing, notification | Subscription UUID zaten public; yalnız upstream customer/order refs ve event DTO'ları değişir |
| usage-service | billing, notification, cdr-simulator | Subscription UUID akışı korunur; tariff-change içindeki order/customer alanları ve dış simulator kontratı birlikte doğrulanır |
| billing-service | payment, notification | Invoice REST/path; payment `invoiceId`; invoice eventleri ve notification consumer DTO'ları |
| payment-service | order, billing | Payment UUID korunur; `orderId`, `invoiceId`, `customerId` foreign ref semantiği standardize edilir |
| notification-service | Upstream subscription/usage/billing | Kendi PK'ları değişmez; consumer kontratları upstream ile aynı sürümde güncellenir |
| ticket-service | customer-service | Ticket UUID korunur; customer public UUID ile referans kurulur |

## 5. ADIM 2 öncesi kapsam kararları

Görev metnindeki “internal PK'sı Long/BIGSERIAL olan her entity” ifadesi literal uygulanırsa dışarı hiç çıkmayan teknik/child entity'lere de `public_id` eklenir. Kod değişikliğine başlamadan önce aşağıdaki kapsamın onaylanması gerekir:

| Kategori | Long PK entity'leri | Öneri |
|---|---|---|
| Dışarı çıkan aggregate/child | `Customer`, `Tariff`, `Addon`, `Order`, `OrderItem`, `Invoice` | `public_id` kesin eklenmeli |
| Başka servise referans kaynağı olabilecek domain kayıtları | `BillCycle` | Dış API/eventte kendi ID'si yok; gelecekte sınır aşacaksa eklenmeli |
| Yalnız internal child/join | `Address`, `Document`, `InvoiceLine`, `TariffAddon` bileşik anahtarı | Mevcut sınırda ID sızmıyor; public ID ancak “her entity” literal kapsamı isteniyorsa eklenmeli |
| Teknik entity | `OtpAttempt`, `SagaState`, Long-PK outbox kayıtları | Public API kimliği değildir; public ID eklenmemesi önerilir |

Önerilen uygulama kapsamı: public ID'yi dışarı çıkan veya başka servis tarafından referans verilen domain kaynaklarına eklemek; outbox, saga, OTP deneme kaydı ve yalnız internal join kayıtlarını internal bırakmaktır. Bu seçim, gereksiz UUID kolon/index maliyetini önlerken görevin güvenlik ve kontrat hedefini karşılar.

## 6. Onay kapısı

ADIM 2–4'e henüz geçilmemiştir. Onay sonrasında servisler görevde verilen bağımlılık sırasıyla ele alınmalı; her servis ayrı derlenip test edilmeli ve event producer/consumer çiftleri aynı değişiklik diliminde güncellenmelidir.

Özellikle uygulama başlamadan şu iki nokta onaylanmalıdır:

1. `public_id` kapsamı yukarıdaki önerilen domain sınırı mı olacak, yoksa Long PK'li teknik/internal entity'ler dahil literal “her entity” kapsamı mı?
2. `payment-service.customerId` için hedef UUID Customer public ID mi, yoksa API Gateway'den gelen Keycloak subject/user ID mi? Mevcut kod ikisini ayırmıyor.
