# TelcoX CRM - Kubernetes (Minikube) Deployment

Bu dizin, TelcoX CRM mikroservislerinin Kubernetes (Minikube) üzerinde çalıştırılması için gerekli manifestoları içerir.
Veritabanları ve altyapı bileşenleri (PostgreSQL, Kafka, Redis, Zipkin, Keycloak, MinIO vb.) host makinede `docker-compose` ile çalışmaya devam edecektir. K8s pod'ları bu bileşenlere Minikube'ün yerleşik `host.minikube.internal` DNS adresi üzerinden erişir.

**Önemli Not:** Bu yapılandırma Minikube'ün **Docker sürücüsü** (`--driver=docker`) ile kullanıldığı varsayımıyla hazırlanmıştır. Eğer farklı bir sürücü (virtualbox, hyperv vb.) kullanılıyorsa DNS çözümü farklılık gösterebilir.

## 1. Altyapıyı Başlatma
Öncelikle host makinedeki docker-compose altyapısının ayakta olduğundan emin olun (veritabanları, keycloak, kafka vs).

```bash
cd ..
docker-compose up -d
```

## 2. Minikube İmajlarının Oluşturulması
Projede tek bir paylaşımlı `Dockerfile` bulunmaktadır. İmajları doğrudan Minikube'ün Docker daemon'u üzerinde oluşturmak için aşağıdaki komutları proje ana dizininde (bu README'nin bir üst dizininde) çalıştırın:

```bash
# Servislerin listesi: api-gateway, discovery-server, config-server, identity-service, customer-service, product-catalog-service, order-service, subscription-service, usage-service, billing-service, payment-service, notification-service, ticket-service

# Örnek: Tüm servisleri build etmek için (Powershell)
$services = "api-gateway", "discovery-server", "config-server", "identity-service", "customer-service", "product-catalog-service", "order-service", "subscription-service", "usage-service", "billing-service", "payment-service", "notification-service", "ticket-service"

foreach ($svc in $services) {
    minikube image build --build-arg SERVICE=$svc -t telcox/${svc}:latest .
}
``` 

# Örnek: Yukarıdaki çalışmazsa

``` 
& "C:\Program Files\Kubernetes\Minikube\minikube.exe" docker-env | Invoke-Expression
``` 

``` 
foreach ($svc in $services) {
    minikube image build --build-arg SERVICE=$svc -t telcox/${svc}:latest .
}
```

## 3. Namespace ve Secret'ların Oluşturulması

İlk olarak `telco-crm` namespace'ini oluşturun:
```bash
kubectl apply -f k8s/namespace.yaml
```

**Güvenlik:**
`customer-service` ve `identity-service` için gerekli hassas bilgiler (AES-GCM anahtarı ve Keycloak Client Secret) Kubernetes Secret olarak tanımlanmıştır. `k8s/customer-service/secret.yaml` ve `k8s/identity-service/secret.yaml` dosyalarında yer tutucu (placeholder) değerler vardır. Gerçek değerleri ayarlamak için bu dosyaları düzenleyebilir veya terminalden doğrudan şu komutlarla oluşturabilirsiniz (var olan placeholder secretları ezmek için):

```bash
# Customer Service (PiiConverter) AES_GCM_KEY (32 karakterlik bir anahtar)
kubectl create secret generic customer-service-secret --from-literal=AES_GCM_KEY="gercek-32-karakterli-gizli-anahtar" -n telco-crm --dry-run=client -o yaml | kubectl apply -f -

# Identity Service KEYCLOAK_CLIENT_SECRET
kubectl create secret generic identity-service-secret --from-literal=KEYCLOAK_CLIENT_SECRET="gercek-keycloak-client-secret" -n telco-crm --dry-run=client -o yaml | kubectl apply -f -
```

## 4. Servislerin Başlatılması

Öncelikle altyapı servislerini (config ve discovery) ayağa kaldırın:
```bash
kubectl apply -f k8s/discovery-server/
kubectl apply -f k8s/config-server/
```

Bu servislerin `Running` durumuna gelmesini bekleyin:
```bash
kubectl get pods -n telco-crm -w
```

Ardından gateway'i ve diğer tüm iş servislerini başlatın:
```bash
kubectl apply -f k8s/api-gateway/
kubectl apply -f k8s/identity-service/
kubectl apply -f k8s/customer-service/
kubectl apply -f k8s/product-catalog-service/
kubectl apply -f k8s/order-service/
kubectl apply -f k8s/subscription-service/
kubectl apply -f k8s/usage-service/
kubectl apply -f k8s/billing-service/
kubectl apply -f k8s/payment-service/
kubectl apply -f k8s/notification-service/
kubectl apply -f k8s/ticket-service/
```

## 5. Uygulamaya Erişim

Sadece `api-gateway` NodePort üzerinden dışarı açılmıştır. Minikube üzerinden Gateway'in URL'ini almak için:

```bash
minikube service api-gateway -n telco-crm --url
```

Bu URL üzerinden isteklerinizi gerçekleştirebilirsiniz. Diğer tüm servisler K8s ağı içinde kapalıdır (ClusterIP) ve sadece birbirleriyle (Gateway dahil) iletişim kurabilirler.
