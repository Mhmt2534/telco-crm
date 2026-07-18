import json
import time
import subprocess
import requests
import random
import uuid
from datetime import datetime

# Configuration
GATEWAY_URL = "http://localhost:8080"
CUSTOMER_SERVICE_URL = f"{GATEWAY_URL}/api/v1/customers"
KAFKA_CONTAINER = "telco-kafka"
MINIO_API_URL = "http://localhost:9000"

def get_current_day_of_month():
    return datetime.now().day

def generate_valid_tckn():
    digits = [random.randint(1, 9)] + [random.randint(0, 9) for _ in range(8)]
    sum_odd = sum(digits[i] for i in range(0, 9, 2))
    sum_even = sum(digits[i] for i in range(1, 8, 2))
    d10 = ((sum_odd * 7) - sum_even) % 10
    digits.append(d10)
    d11 = sum(digits) % 10
    digits.append(d11)
    return "".join(map(str, digits))

def query_db(db_container, database, query):
    try:
        cmd = [
            "docker", "exec", "-i", db_container,
            "psql", "-U", "postgres", "-d", database, "-c", query
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout
    except Exception as e:
        print(f"Error querying db {database}: {e}")
        return ""

def curl_container_api(container_name, port, path, method="GET", payload=None):
    try:
        url = f"http://localhost:{port}{path}"
        if method.upper() == "GET":
            r = requests.get(url, timeout=10)
        elif method.upper() == "POST":
            r = requests.post(url, json=payload, timeout=10)
        elif method.upper() == "PUT":
            r = requests.put(url, json=payload, timeout=10)
        elif method.upper() == "DELETE":
            r = requests.delete(url, timeout=10)
        else:
            r = requests.request(method, url, json=payload, timeout=10)
        return r.text
    except Exception as e:
        print(f"Error calling local API at port {port}: {e}")
        return None

def publish_kafka_message(topic, key, payload):
    try:
        cmd = [
            "docker", "exec", "-i", KAFKA_CONTAINER,
            "/opt/bitnami/kafka/bin/kafka-console-producer.sh",
            "--bootstrap-server", "kafka:29092",
            "--topic", topic,
            "--property", "parse.key=true",
            "--property", "key.separator=:"
        ]
        input_str = f"{key}:{json.dumps(payload)}\n"
        subprocess.run(cmd, input=input_str, text=True, check=True)
    except Exception as e:
        print(f"Error publishing to Kafka: {e}")

def main():
    print("=== KART 27: Kota Aşım (Overage) Ücretlendirmesi E2E Testi ===\n")

    # 1. Yeni test müşterisi oluşturma
    print("1. Yeni test müşterisi oluşturuluyor...")
    phone_num = f"90533{random.randint(1000000, 9999999)}"
    email = f"test_{random.randint(1000, 9999)}@telcox.com"
    tckn = generate_valid_tckn()
    
    customer_payload = {
        "firstName": "Selin",
        "lastName": "Yilmaz",
        "type": "INDIVIDUAL",
        "identityNumber": tckn,
        "dateOfBirth": "1995-10-12",
        "phone": phone_num,
        "email": email,
        "addresses": [
            {
                "line1": "TelcoX Buyukdere Cad No 2",
                "city": "Istanbul",
                "district": "Sisli",
                "postalCode": "34394",
                "isDefault": True
            }
        ]
    }
    
    resp_str = curl_container_api("customer-service", 9002, "/api/v1/customers", "POST", customer_payload)
    if not resp_str:
        print("   [FAIL] Müşteri oluşturulamadı!")
        return
        
    try:
        customer = json.loads(resp_str)
        customer_id = customer["id"]
    except Exception as e:
        print(f"   [FAIL] Müşteri yanıtı ayrıştırılamadı: {resp_str}, Hata: {e}")
        return
        
    print(f"   Müşteri başarıyla oluşturuldu. ID: {customer_id}")

    # Temizlik ve Değişkenler
    current_day = get_current_day_of_month()
    test_sub_id = str(uuid.uuid4())
    fixed_amount = 100.00

    # Query customer PK id (bigint) from customer-db using public_id
    customer_pk = None
    try:
        cmd = [
            "docker", "exec", "-i", "customer-db",
            "psql", "-U", "postgres", "-d", "customer_db",
            "-t", "-A", "-c", f"SELECT id FROM customer WHERE public_id = '{customer_id}';"
        ]
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        customer_pk = int(res.stdout.strip())
        print(f"   [INFO] Customer DB PK: {customer_pk}")
    except Exception as e:
        print(f"   [FAIL] Failed to retrieve customer PK: {e}")
        return
    
    print("   Eski test fatura döngüleri temizleniyor...")
    query_db("billing-db", "billing_db", "DELETE FROM bill_cycle WHERE msisdn LIKE '90533%';")

    # 2. BillCycle kaydı ekleme
    print(f"2. billing_db'ye BillCycle kaydı ekleniyor (cut_off_day: {current_day})...")
    sql_billcycle = (
        f"INSERT INTO bill_cycle (customer_id, customer_public_id, subscription_id, msisdn, cut_off_day, fixed_amount) "
        f"VALUES ({customer_pk}, '{customer_id}', '{test_sub_id}', '{phone_num}', {current_day}, {fixed_amount});"
    )
    query_db("billing-db", "billing_db", sql_billcycle)

    # 3. Quotas kaydı ekleme
    print("3. usage_db'ye 100 MB Quota kaydı tanımlanıyor...")
    sql_quota_insert = (
        f"INSERT INTO quotas (id, subscription_id, period_start, period_end, total_minutes, total_sms, total_mb, "
        f"minutes_remaining, sms_remaining, mb_remaining, created_at, updated_at, voice_threshold_reached, sms_threshold_reached, data_threshold_reached, "
        f"voice_exceeded, sms_exceeded, data_exceeded, version) "
        f"VALUES ('{uuid.uuid4()}', '{test_sub_id}', '2026-07-01 00:00:00+00', '2026-07-31 23:59:59+00', 100, 100, 100, "
        f"100, 100, 100, NOW(), NOW(), false, false, false, false, false, false, 1);"
    )
    query_db("usage-db", "usage_db", sql_quota_insert)

    # 4. Kota İçinde CDR kullanımı (80 MB)
    print("4. Kafka'ya kota sınırları içinde 80 MB DATA kullanımı yollanıyor...")
    cdr_80_payload = {
        "subscriptionId": test_sub_id,
        "msisdn": phone_num,
        "type": "DATA",
        "amount": 80.0,
        "cdrRef": f"cdr-ref-{uuid.uuid4()}",
        "recordedAt": datetime.utcnow().isoformat() + "Z"
    }
    publish_kafka_message("telco.usage.events", phone_num, cdr_80_payload)

    # Quota remaining verify (80 MB harcandı, 20 MB kalmalı)
    quota_80_ok = False
    for _ in range(15):
        db_check = query_db("usage-db", "usage_db", f"SELECT mb_remaining FROM quotas WHERE subscription_id='{test_sub_id}';")
        if "20" in db_check:
            quota_80_ok = True
            break
        time.sleep(1)
        
    if quota_80_ok:
        print("   [PASS] quotas tablosunda mb_remaining=20 olarak doğrulandı.")
    else:
        print(f"   [FAIL] quotas tablosunda mb_remaining=20 olamadı! Son çıktı: {db_check.strip()}")
        return

    # 5. Sınır CDR kullanımı (40 MB, kota 20 MB kalmıştı -> 20 MB aşım olmalı)
    print("5. Kafka'ya kota sınırını aşacak (sınırda) 40 MB DATA kullanımı yollanıyor (20 MB aşım olmalı)...")
    cdr_boundary_payload = {
        "subscriptionId": test_sub_id,
        "msisdn": phone_num,
        "type": "DATA",
        "amount": 40.0,
        "cdrRef": f"cdr-ref-{uuid.uuid4()}",
        "recordedAt": datetime.utcnow().isoformat() + "Z"
    }
    publish_kafka_message("telco.usage.events", phone_num, cdr_boundary_payload)

    # Overage records verify (overage=t, overage_amount=20)
    boundary_ok = False
    for _ in range(15):
        db_check = query_db("usage-db", "usage_db", 
            f"SELECT overage, overage_amount FROM usage_records WHERE cdr_ref='{cdr_boundary_payload['cdrRef']}';")
        if "t" in db_check and "20" in db_check:
            boundary_ok = True
            break
        time.sleep(1)

    if boundary_ok:
        print("   [PASS] usage_records tablosunda overage=true ve overage_amount=20 olarak doğrulandı.")
    else:
        print(f"   [FAIL] usage_records tablosunda sınırda aşım doğrulanamadı! Son çıktı: {db_check.strip()}")
        return

    # 6. Tamamen Aşım CDR kullanımı (30 MB, kota 0 kalmıştı -> 30 MB aşım olmalı)
    print("6. Kafka'ya tamamen aşımda olan 30 MB DATA kullanımı yollanıyor (30 MB aşım olmalı)...")
    cdr_overage_payload = {
        "subscriptionId": test_sub_id,
        "msisdn": phone_num,
        "type": "DATA",
        "amount": 30.0,
        "cdrRef": f"cdr-ref-{uuid.uuid4()}",
        "recordedAt": datetime.utcnow().isoformat() + "Z"
    }
    publish_kafka_message("telco.usage.events", phone_num, cdr_overage_payload)

    # Overage records verify (overage=t, overage_amount=30)
    overage_ok = False
    for _ in range(15):
        db_check = query_db("usage-db", "usage_db", 
            f"SELECT overage, overage_amount FROM usage_records WHERE cdr_ref='{cdr_overage_payload['cdrRef']}';")
        if "t" in db_check and "30" in db_check:
            overage_ok = True
            break
        time.sleep(1)

    if overage_ok:
        print("   [PASS] usage_records tablosunda overage=true ve overage_amount=30 olarak doğrulandı.")
    else:
        print(f"   [FAIL] usage_records tablosunda tam aşım doğrulanamadı! Son çıktı: {db_check.strip()}")
        return

    # Toplam aşım = 20 + 30 = 50 MB. Aşım ücreti = 50 * 0.05 = 2.50 TRY. Toplam fatura = 100.00 + 2.50 = 102.50 TRY.

    # 7. billing-service runs API tetikleme
    print("\n7. billing-service runs API tetikleniyor...")
    run_resp = None
    for _ in range(15):
        run_resp = curl_container_api("billing-service", 9007, "/api/v1/billing/runs", "POST")
        if run_resp:
            break
        time.sleep(2)
    print(f"   API Yanıtı: {run_resp}")

    # Faturanın oluştuğunu ve tutarını doğrulama (102.50 TRY)
    print("8. billing_db'den oluşan fatura tutarı sorgulanıyor...")
    invoice_id = None
    invoice_uuid = None
    invoice_amount_ok = False
    for _ in range(45):
        db_out = query_db("billing-db", "billing_db", 
            f"SELECT id, public_id, amount, status FROM invoice WHERE customer_public_id='{customer_id}' LIMIT 1;")
        if "102.50" in db_out:
            invoice_amount_ok = True
            for line in db_out.splitlines():
                if "102.50" in line and "|" in line:
                    parts = line.split("|")
                    invoice_id = int(parts[0].strip())
                    invoice_uuid = parts[1].strip()
                    break
            break
        time.sleep(1)

    if invoice_amount_ok:
        print(f"   [PASS] Fatura başarıyla oluşturuldu. ID: {invoice_id}, UUID: {invoice_uuid}, Tutar: 102.50 TRY")
    else:
        print(f"   [FAIL] Fatura 102.50 TRY olarak oluşamadı! Son çıktı: {db_out.strip()}")
        return

    # Fatura kalemlerini doğrulama (Monthly Fixed + DATA Overage)
    print("9. Fatura kalemleri sorgulanıyor...")
    lines_check = query_db("billing-db", "billing_db", 
        f"SELECT description, unit_price, line_total FROM invoice_line WHERE invoice_id={invoice_id};")
    print(lines_check.strip())
    
    if "Monthly Fixed Tariff Fee" in lines_check and "DATA Overage Charge" in lines_check and "2.50" in lines_check:
        print("   [PASS] Fatura kalemleri (sabit tarife ve kota aşım satırı) başarıyla doğrulandı.")
    else:
        print("   [FAIL] Fatura kalemleri doğrulanamadı!")
        return

    # 10. PDF Üretim doğrulaması
    print("10. MinIO üzerinde fatura PDF dosyasının oluşumu bekleniyor...")
    pdf_found = False
    
    for _ in range(45):
        try:
            pdf_resp_str = curl_container_api("billing-service", 9007, f"/api/v1/invoices/{invoice_uuid}/pdf", "GET")
            if pdf_resp_str and "pdfUrl" in pdf_resp_str:
                presigned_data = json.loads(pdf_resp_str)
                raw_url = presigned_data["pdfUrl"]
                download_url = raw_url.replace("telco-minio", "localhost")
                r = requests.get(download_url, timeout=5)
                if r.status_code == 200 and r.content.startswith(b"%PDF"):
                    pdf_found = True
                    break
                else:
                    print(f"      [DEBUG] GET download_url status: {r.status_code}, content: {r.text[:200]}")
            else:
                print(f"      [DEBUG] GET pdfUrl endpoint returned: {pdf_resp_str}")
        except Exception as e:
            print(f"      [DEBUG] S3 Download exception: {e}")
        time.sleep(1)

    if pdf_found:
        print("   [PASS] Fatura PDF'i MinIO'da başarıyla doğrulandı.")
    else:
        print("   [FAIL] Fatura PDF'i bulunamadı veya geçersiz!")
        return

    # 11. Ödeme tamamlama
    print("11. Ödeme simülasyonu tetikleniyor...")
    payment_payload = {
        "eventType": "PaymentCompleted",
        "paymentId": str(uuid.uuid4()),
        "orderId": str(uuid.uuid4()),
        "invoiceId": invoice_uuid,
        "customerId": customer_id,
        "amount": 102.50
    }
    publish_kafka_message("telcox.Payment.events", invoice_uuid, payment_payload)

    # Fatura PAID doğrulaması
    paid_verified = False
    for _ in range(15):
        paid_check = query_db("billing-db", "billing_db", f"SELECT status FROM invoice WHERE id={invoice_id};")
        if "PAID" in paid_check and "UNPAID" not in paid_check:
            paid_verified = True
            break
        time.sleep(1)

    if paid_verified:
        print("   [PASS] Fatura durumu başarıyla PAID olarak güncellendi.")
    else:
        print(f"   [FAIL] Fatura durumu güncellenemedi! Son durum: {paid_check.strip()}")
        return

    print("\n=== KART 27: KOTA AŞIM E2E ENTEGRASYON TESTİ BAŞARIYLA GEÇTİ (100% OK) ===")

if __name__ == "__main__":
    main()
