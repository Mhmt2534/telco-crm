# Adım 3 — Public ID Geçişi ve Kontrat Düzeltmeleri

## Uygulama ilkesi

- Servis sınırlarından geçen kimlikler `UUID public_id` oldu.
- Entity'lerin internal `Long` PK'ları değiştirilmedi.
- `SagaState.orderId`, outbox kayıt PK'ları, `Address`, `Document`, `TariffAddon`, `BillCycle`, `InvoiceLine` için yeni bir `public_id` üretilmedi.
- Başka servislere ait referanslar için yeni `*_public_id UUID` kolonları eklendi. Eski kolonlar, dağıtık backfill tamamlanana kadar nullable legacy kolon olarak bırakıldı.
- `payment.customerId`, customer-service public UUID'sidir. Gateway'den gelen Keycloak subject değeri yalnızca `Payment.actorId` alanında tutulur.

## 1. customer-service

### Değişen dosyalar

- `CustomerController`, `CustomerService`, `CustomerRepository`
- `CustomerResponse`, `CustomerMapper`
- `CustomerRegisteredEvent`, `CustomerUpdatedEvent`, `CustomerKYCApprovedEvent`
- `OutboxEventPublisher`
- `CustomerEntityTest`

### Değişiklik

- Response `id`, `Customer.publicId` üzerinden dolduruluyor.
- GET/PUT/DELETE path parametreleri `UUID` kabul ediyor ve `findByPublicId` kullanıyor.
- Customer eventlerinin `customerId` ve outbox `aggregateId` değerleri public UUID oldu.

### Kontrat

```text
Eski: { customerId: Long, ... }
Yeni: { customerId: UUID, ... }
```

### Test

- Temiz derleme başarılı.
- 2 test çalıştı, 2 test geçti.
- Flyway V1–V2 doğrulandı.

## 2. product-catalog-service

### Değişen dosyalar

- `TariffController`, `AddonController`, `ProductController`, `InternalCatalogController`
- `TariffService`, `AddonService`, `OutboxEventPublisher`
- `TariffRepository`, `AddonRepository`, `CatalogMapper`
- `TariffResponse`, `AddonResponse`, `ProductResponse`, `TariffAddonRequest`, `AddonRequest`

### Değişiklik

- Tariff ve Addon response/path/repository yüzeyleri public UUID oldu.
- Batch product Feign yüzeyi `codes` yerine `ids: List<UUID>` kullanıyor.
- Tariff–Addon ilişki istekleri UUID kullanıyor; `TariffAddon` internal PK yapısı değişmedi.
- Usage-service çağrısı, kendi sırası geldiğinde `newTariffId` ile güncellendi.

### Kontrat

```text
Tariff event eski: { code, ... }
Tariff event yeni: { tariffId: UUID, code, ... }
```

### Test

- `mvnw -pl product-catalog-service -am clean test`: başarılı.
- Serviste test kaynağı yok; ana kaynakların temiz derlemesi geçti.

## 3. order-service ve senkron consumer'lar

### Değişen dosyalar

- Order: controller, service, saga orchestrator, mapper ve repository katmanları
- `Order`, `OrderItem`; bütün order request/response DTO'ları
- Customer/Product Catalog Feign client'ları, DTO ve fallback'leri
- `OutboxEventPublisher`, payment/subscription event consumer'ları ve payload DTO'ları
- Order testleri
- Payment: `OrderCreatedEvent`, payment entity/repository/service ve event DTO'ları
- Subscription: `OrderConfirmedEvent`, subscription entity/service/response ve order consumer
- Migration'lar:
  - `order-service/.../V5__add_public_cross_service_references.sql`
  - `payment-service/.../V4__add_public_cross_service_references.sql`
  - `subscription-service/.../V4__add_public_cross_service_references.sql`

### Değişiklik

- Order REST response/path/customer query alanları public UUID oldu.
- OrderItem REST referansı `productId: UUID`; `productCode` yalnızca açıklayıcı iş alanı olarak korunuyor.
- Customer ve catalog Feign çağrıları UUID gönderiyor.
- Event consumer UUID ile `Order.publicId` arıyor; yalnızca yerel saga erişiminde internal `Order.id` kullanıyor.

### OrderCreated

```text
Eski:
{
  orderId: Long,
  customerId: Long,
  items: [{ productCode, quantity, unitPrice }],
  totalAmount, currency, occurredAt
}

Yeni:
{
  orderId: UUID,
  customerId: UUID,
  items: [{ productId: UUID, productCode, quantity, unitPrice }],
  totalAmount, currency, occurredAt
}
```

### OrderConfirmed

```text
Eski:
{
  orderId: Long,
  customerId: Long,
  tariffCode,
  productType,
  subscriptionId?: UUID
}

Yeni:
{
  orderId: UUID,
  customerId: UUID,
  productId: UUID,
  tariffCode,
  productType,
  subscriptionId?: UUID
}
```

### TariffChangeRequested

```text
Eski: { orderId: Long, customerId: Long, oldTariffCode, newTariffCode, ... }
Yeni: { orderId: UUID, customerId: UUID, oldTariffId: UUID, newTariffId: UUID,
        oldTariffCode, newTariffCode, ... }
```

### Test

- order-service: 3 test çalıştı, 3 test geçti.
- PaymentCompleted Spring Cloud Contract stub, UUID order kimliğiyle uçtan uca geçti.
- payment-service: 7 test çalıştı, 7 test geçti.
- subscription-service: temiz test/derleme başarılı; test kaynağı yok.

## 4. billing-service ve notification-service

### Değişen dosyalar

- Billing: `Invoice`, `BillCycle`, `InvoiceRepository`, `InvoiceController`
- `CustomerClient`, billing customer/payment/subscription DTO'ları
- `BillRunScheduler`, `InvoiceOverdueScheduler`, `InvoicePdfService`
- `SubscriptionActivatedConsumer`, `PaymentEventsConsumer`, `InvoiceGeneratedConsumer`
- `V4__add_public_customer_references.sql`
- Notification consumer DTO'ları değişmedi; UUID uyumluluğu derleme ile doğrulandı.

### Değişiklik

- Invoice PDF path ve repository sorgusu `Invoice.publicId` kullanıyor.
- Customer Feign çağrısı customer public UUID gönderiyor.
- Invoice eventlerinin `invoiceId`, `customerId` ve `aggregateId` alanları UUID oldu.
- Event adı `InvoiceGeneratedEvent` yerine ortak sabitle uyumlu `InvoiceGenerated` oldu.

### InvoiceGenerated

```text
Eski: eventType=InvoiceGeneratedEvent,
      { invoiceId: Long, customerId: Long, subscriptionId: UUID, ... }

Yeni: eventType=InvoiceGenerated,
      { invoiceId: UUID, customerId: UUID, subscriptionId: UUID, ... }
```

`InvoicePaid` ve `InvoiceOverdue` payload'ları da aynı UUID kuralını izliyor.

### Test

- billing-service: temiz test/derleme başarılı; test kaynağı yok.
- notification-service: temiz test/derleme başarılı; test kaynağı yok.

## 5. Bug düzeltmeleri

### a. SubscriptionActivated eksik customerId

```text
Eski:
{ orderId: Long, subscriptionId: UUID, msisdn, status }

Yeni:
{ orderId: UUID, subscriptionId: UUID, customerId: UUID, msisdn, status }
```

- Producer gerçek `Subscription.customerId` değerini üretir.
- Order, billing ve notification consumer'ları UUID ile uyumludur.

### b. PaymentCompleted tek kontrat

```text
Eski şekil 1 (invoice):
{ paymentId, invoiceId: String, amount, currency, paidAt }

Eski şekil 2 (order):
{ paymentId, orderId: Long, customerId: String, amount, occurredAt }

Yeni tek şekil:
{
  paymentId: UUID,
  orderId?: UUID,
  invoiceId?: UUID,
  customerId: UUID,
  amount,
  occurredAt
}
```

- Order ödemesinde `orderId`, fatura ödemesinde `invoiceId` doludur.
- Normal ödeme ve retry producer'ı aynı `PaymentCompletedEvent` DTO'sunu kullanır.
- Order ve billing consumer'ları aynı DTO şekline göre çalışır.
- Spring Cloud Contract bu tek şekle güncellendi.

### c. ticket-service customerId

- Ticket entity/request/response zaten UUID kullanıyordu.
- Customer Feign client veya customer event consumer bulunmadı; değiştirilecek hatalı entegrasyon noktası yoktu.
- Temiz test/derleme başarılı.

### d. Billing → notification invoice/customer kimlikleri

- Billing artık `invoice.publicId` ve customer public UUID üretiyor.
- Notification DTO'sunun zaten `UUID invoiceId/customerId` beklediği doğrulandı.

## 6. payment-service customer ve actor ayrımı

- `Payment.customerId: UUID` ve `Wallet.customerId: UUID`, customer-service public ID'sidir.
- Order ödemelerinde kaynak `OrderCreated.customerId` alanıdır.
- Doğrudan fatura ödemelerinde kaynak `PaymentRequest.customerId` alanıdır.
- `X-User-Id` artık customerId olarak yazılmaz; `Payment.actorId` alanına yazılır.
- `Payment.invoiceId` ve `Payment.orderId` public UUID referanslarıdır.

## Usage-service takip düzeltmesi

- Tariff-change event consumer `newTariffId: UUID` okuyor.
- Product catalog Feign çağrısı `/api/v1/tariffs/{publicId}` yolunu kullanıyor.
- Temiz test/derleme başarılı; test kaynağı yok.

## Genel test özeti

| Servis | Sonuç | Test sayısı |
|---|---:|---:|
| customer-service | Başarılı | 2 |
| product-catalog-service | Başarılı | 0 |
| order-service | Başarılı | 3 |
| payment-service | Başarılı | 7 |
| subscription-service | Başarılı | 0 |
| billing-service | Başarılı | 0 |
| notification-service | Başarılı | 0 |
| usage-service | Başarılı | 0 |
| ticket-service | Başarılı | 0 |

Toplam: 12 test geçti, başarısız test yok. Test kaynağı olmayan servislerde temiz derleme ve test lifecycle'ı başarıyla tamamlandı.

Ek olarak 18 modülün tamamında `mvnw test -DskipTests` ile ana ve test kaynak derlemesi başarılıdır.

## Kısıt doğrulaması

- Dış sınır taramasında kalan tek `Long orderId`, izin verilen `SagaState.orderId` alanıdır.
- Pure-internal tablolara yeni `public_id` eklenmedi.
- Internal `Long` PK'lar korunmuştur.
- `git diff --check` başarılıdır.
