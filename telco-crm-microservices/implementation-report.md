# Public ID Ayrıştırması — Uygulama Raporu

## Onaylanan kararlar

- `public_id` yalnız servis sınırını geçen domain entity'lerine eklenir; teknik ve pure-internal tablolara eklenmez.
- `payment-service.customerId`, ilerleyen adımlarda Customer public UUID'si olacaktır. Keycloak subject / `X-User-Id` actor kimliği olarak ayrı tutulacaktır.
- `SubscriptionActivated.customerId`, tekil `PaymentCompleted` kontratı ve Ticket→Customer bağımlılığı ADIM 3–4 kapsamında düzeltilecektir.

## ADIM 2 — Public ID kolonları

Durum: Tamamlandı.

Her migration ilgili servis veritabanında `pgcrypto` extension'ını idempotent biçimde açar, mevcut satırları PostgreSQL `gen_random_uuid()` default'u ile doldurur ve named `UNIQUE` constraint ekler. PostgreSQL unique constraint'i kendi unique index'ini oluşturur. Java entity'leri de veritabanı dışındaki yeni nesne oluşturma/test akışlarında null kimlik oluşmaması için `UUID.randomUUID()` ile başlangıç değeri üretir.

### identity-service

Değişiklik yok. `OtpAttempt.id` servis sınırından çıkmadığı için onaylanan kapsam dışında bırakıldı.

Doğrulama:

- `./mvnw.cmd -pl identity-service -am test` — başarılı; test bulunmuyor.

### customer-service

Eklenen migration:

- `customer-service/src/main/resources/db/migration/V2__add_customer_public_id.sql`

Değişen dosyalar:

- `customer-service/src/main/java/com/telcox/springmicroservices/customer/domain/Customer.java`
- `customer-service/src/main/java/com/telcox/springmicroservices/customer/mapper/CustomerMapper.java`

Kapsam:

- Yalnız `customer.public_id UUID NOT NULL DEFAULT gen_random_uuid()` ve unique constraint eklendi.
- `Address`, `Document` ve outbox teknik PK'si değiştirilmedi.
- Mapper create akışında entity'nin kendi UUID başlangıç değerini korumak için `publicId` ignore edildi.

Doğrulama:

- `./mvnw.cmd -pl customer-service -am test` — test context'i harici PostgreSQL'e bağlanamadığı için `SQLState 08001` ile başarısız; derleme aşaması başarılı.
- `./mvnw.cmd -pl customer-service -am -DskipTests package` — başarılı.

### product-catalog-service

Eklenen migration:

- `product-catalog-service/src/main/resources/db/migration/V4__add_catalog_public_ids.sql`

Değişen dosyalar:

- `product-catalog-service/src/main/java/com/telcox/springmicroservices/productcatalog/domain/Tariff.java`
- `product-catalog-service/src/main/java/com/telcox/springmicroservices/productcatalog/domain/Addon.java`
- `product-catalog-service/src/main/java/com/telcox/springmicroservices/productcatalog/mapper/CatalogMapper.java`

Kapsam:

- `tariff.public_id` ve `addon.public_id` eklendi.
- `TariffAddon`, outbox ve code/version internal ilişkileri değiştirilmedi.
- Mapper create akışlarında entity UUID başlangıç değerleri korunuyor.

Doğrulama:

- `./mvnw.cmd -pl product-catalog-service -am test` — başarılı; test bulunmuyor.
- Mapper güncellemesinden sonra `./mvnw.cmd -pl product-catalog-service -am -DskipTests package` — başarılı.

### order-service

Eklenen migration:

- `order-service/src/main/resources/db/migration/V4__add_order_public_ids.sql`

Değişen dosyalar:

- `order-service/src/main/java/com/telcox/springmicroservices/orderservice/domain/entity/Order.java`
- `order-service/src/main/java/com/telcox/springmicroservices/orderservice/domain/entity/OrderItem.java`
- `order-service/src/main/java/com/telcox/springmicroservices/orderservice/mapper/OrderMapper.java`

Kapsam:

- `orders.public_id` ve REST response'ta kendi ID'si bulunan `order_item.public_id` eklendi.
- `SagaState`, outbox ve internal `order_id BIGINT` join'i değiştirilmedi.
- Mapper create akışlarında entity UUID başlangıç değerleri korunuyor.

Doğrulama:

- `./mvnw.cmd -pl order-service -am test` — başarılı; 3 test geçti.

### billing-service

Eklenen migration:

- `billing-service/src/main/resources/db/migration/V3__add_invoice_public_id.sql`

Değişen dosyalar:

- `billing-service/src/main/java/com/telcox/springmicroservices/billing/entity/Invoice.java`

Kapsam:

- Yalnız `invoice.public_id` eklendi.
- Kendi kimliği servis sınırından çıkmayan `BillCycle`, `InvoiceLine` ve outbox teknik PK'si değiştirilmedi.

Doğrulama:

- `./mvnw.cmd -pl billing-service -am test` — başarılı; test bulunmuyor.

### UUID-PK servisleri

`subscription-service`, `usage-service`, `payment-service`, `notification-service` ve `ticket-service` ana dış entity PK'ları zaten UUID olduğu için migration eklenmedi. `identity-service` yukarıdaki kapsam kararıyla atlandı.

## ADIM 2'de değişmeyen kontratlar

Bu adımda REST response/path, Feign veya Kafka/outbox payload kontratı değiştirilmedi. Dolayısıyla event producer/consumer değişiklik tablosu boş kalır. Bu sınırlar ADIM 3'te producer ve consumer birlikte değiştirilerek UUID'ye geçirilecektir.
