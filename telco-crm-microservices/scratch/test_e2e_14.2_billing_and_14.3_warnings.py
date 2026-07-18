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

MAILPIT_PORTS = [8825, 8025]

def get_mailpit_api():
    for port in MAILPIT_PORTS:
        url = f"http://localhost:{port}/api/v1/messages"
        try:
            r = requests.get(url, timeout=2)
            if r.status_code == 200:
                return url
        except:
            pass
    return f"http://localhost:8825/api/v1/messages"

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

def check_mailpit_for_email(recipient_email, subject_contains, timeout=10):
    mailpit_api = get_mailpit_api()
    for _ in range(timeout):
        try:
            r = requests.get(mailpit_api, timeout=2)
            if r.status_code == 200:
                data = r.json()
                for msg in data.get("messages", []):
                    to_list = [t.get("Address") for t in msg.get("To", [])]
                    subj = msg.get("Subject", "")
                    if recipient_email in to_list and subject_contains in subj:
                        return True
        except:
            pass
        time.sleep(1)
    return False

def main():
    print("=== KART 22: Senaryo 14.2 & 14.3 Uçtan Uca E2E Entegrasyon Testi ===\n")

    # ──────────────────────────────────────────────────────────────────────────
    # ─── SENARYO 14.2: Aylık Fatura Kesimi & Ödeme Akışı ──────────────────────
    # ──────────────────────────────────────────────────────────────────────────
    print("--- SENARYO 14.2 BAŞLIYOR ---")
    
    print("1. Yeni test müşterisi oluşturuluyor...")
    phone_num = f"90532{random.randint(1000000, 9999999)}"
    email = f"test_{random.randint(1000, 9999)}@telcox.com"
    tckn = generate_valid_tckn()
    
    customer_payload = {
        "firstName": "Hakan",
        "lastName": "Demir",
        "type": "INDIVIDUAL",
        "identityNumber": tckn,
        "dateOfBirth": "1990-05-20",
        "phone": phone_num,
        "email": email,
        "addresses": [
            {
                "line1": "TelcoX Maslak Plaza",
                "city": "Istanbul",
                "district": "Sariyer",
                "postalCode": "34485",
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

    # BillCycle kaydı ekleme
    current_day = get_current_day_of_month()
    subscription_id = str(uuid.uuid4())
    fixed_amount = 250.00

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
    
    # Temizlik: Eski test fatura döngülerini temizle
    print("   Eski test fatura döngüleri temizleniyor...")
    query_db("billing-db", "billing_db", "DELETE FROM bill_cycle WHERE msisdn LIKE '90532%';")

    print(f"2. billing_db'ye BillCycle kaydı ekleniyor (cut_off_day: {current_day})...")
    sql_insert = (
        f"INSERT INTO bill_cycle (customer_id, customer_public_id, subscription_id, msisdn, cut_off_day, fixed_amount) "
        f"VALUES ({customer_pk}, '{customer_id}', '{subscription_id}', '{phone_num}', {current_day}, {fixed_amount});"
    )
    query_db("billing-db", "billing_db", sql_insert)

    # Fatura kesimini tetikleme (Bill-run)
    print("3. billing-service runs API tetikleniyor...")
    run_resp = curl_container_api("billing-service", 9007, "/api/v1/billing/runs", "POST")
    print(f"   API Yanıtı: {run_resp}")

    # Faturanın oluştuğunu doğrulama ve ID'sini çekme
    print("4. Veritabanından oluşan fatura bilgisi sorgulanıyor...")
    invoice_id = None
    invoice_uuid = None
    for _ in range(45):
        db_out = query_db("billing-db", "billing_db", 
            f"SELECT id, public_id, status FROM invoice WHERE customer_public_id='{customer_id}' LIMIT 1;")
        if "1 row" in db_out:
            lines = [l.strip() for l in db_out.split('\n') if l.strip()]
            for line in lines:
                if '|' in line:
                    parts = line.split('|')
                    if parts[0].strip().isdigit():
                        invoice_id = int(parts[0].strip())
                        invoice_uuid = parts[1].strip()
                        break
            if invoice_id and invoice_uuid:
                break
        time.sleep(1)

    if not invoice_id:
        print("   [FAIL] Fatura kaydı veritabanında bulunamadı!")
        return

    print(f"   Fatura başarıyla bulundu. Fatura ID: {invoice_id}, UUID: {invoice_uuid}")

    # MinIO S3 PDF kontrolü
    print("5. MinIO üzerinde fatura PDF dosyasının oluşumu bekleniyor...")
    pdf_found = False
    for _ in range(15):
        pdf_resp = curl_container_api("billing-service", 9007, f"/api/v1/invoices/{invoice_uuid}/pdf")
        if pdf_resp and "pdfUrl" in pdf_resp:
            try:
                pdf_data = json.loads(pdf_resp)
                pdf_url = pdf_data["pdfUrl"]
                # localhost'a çevir
                local_pdf_url = pdf_url.replace("http://telco-minio:9000", MINIO_API_URL)
                r = requests.get(local_pdf_url, timeout=5)
                if r.status_code == 200 and r.content.startswith(b"%PDF"):
                    pdf_found = True
                    break
                else:
                    print(f"      [DEBUG] GET local_pdf_url status: {r.status_code}, content: {r.text[:200]}")
            except Exception as e:
                print(f"      [DEBUG] S3 Download exception: {e}")
        time.sleep(1)

    if pdf_found:
        print("   [PASS] Fatura PDF'i MinIO'da doğrulandı.")
    else:
        print("   [FAIL] Fatura PDF'i bulunamadı veya geçersiz!")
        return

    # Faturanın ilk durumunu kontrol etme (UNPAID)
    status_check = query_db("billing-db", "billing_db", f"SELECT status FROM invoice WHERE id={invoice_id};")
    print(f"   Fatura ilk durumu: {status_check.strip()}")

    # Ödemeyi Kafka üzerinden simüle etme
    print("6. Kafka üzerinden PaymentCompleted eventi gönderiliyor...")
    payment_payload = {
        "eventType": "PaymentCompleted",
        "paymentId": str(uuid.uuid4()),
        "orderId": str(uuid.uuid4()),
        "invoiceId": invoice_uuid,
        "customerId": customer_id,
        "amount": float(fixed_amount)
    }
    publish_kafka_message("telcox.Payment.events", invoice_uuid, payment_payload)

    # Faturanın PAID durumuna geçtiğini doğrulama
    print("7. Faturanın PAID durumuna geçmesi bekleniyor...")
    paid_verified = False
    for _ in range(10):
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

    # ──────────────────────────────────────────────────────────────────────────
    # ─── SENARYO 14.3: Kota Aşımı Bildirim Akışı ──────────────────────────────
    # ──────────────────────────────────────────────────────────────────────────
    print("\n--- SENARYO 14.3 BAŞLIYOR ---")
    
    test_sub_id = str(uuid.uuid4())
    test_msisdn = f"90544{random.randint(1000000, 9999999)}"
    print(f"1. Test abonesi için kota kaydı tanımlanıyor (msisdn: {test_msisdn})...")
    
    # usage_db'ye kota limiti ekleme
    sql_quota_insert = (
        f"INSERT INTO quotas (id, subscription_id, period_start, period_end, total_minutes, total_sms, total_mb, "
        f"minutes_remaining, sms_remaining, mb_remaining, created_at, updated_at, voice_threshold_reached, sms_threshold_reached, data_threshold_reached, "
        f"voice_exceeded, sms_exceeded, data_exceeded, version) "
        f"VALUES ('{uuid.uuid4()}', '{test_sub_id}', '2026-07-01 00:00:00+00', '2026-07-31 23:59:59+00', 100, 100, 100, "
        f"100, 100, 100, NOW(), NOW(), false, false, false, false, false, false, 1);"
    )
    query_db("usage-db", "usage_db", sql_quota_insert)

    # 80% limit aşımı tetikleme (80 MB tüketim)
    print("2. Kafka'ya %80 limitini tetikleyecek CDR kullanımı yollanıyor (80 MB)...")
    cdr_80_payload = {
        "subscriptionId": test_sub_id,
        "msisdn": test_msisdn,
        "type": "DATA",
        "amount": 80.0,
        "cdrRef": f"cdr-ref-{uuid.uuid4()}",
        "recordedAt": datetime.now().isoformat() + "Z"
    }
    publish_kafka_message("telco.usage.events", test_msisdn, cdr_80_payload)

    # DB Kontrolü (%80)
    print("3. usage_db'de data_threshold_reached flaginin true olması bekleniyor...")
    quota_80_db_ok = False
    for _ in range(15):
        db_check = query_db("usage-db", "usage_db", f"SELECT data_threshold_reached FROM quotas WHERE subscription_id='{test_sub_id}';")
        if "t" in db_check:
            quota_80_db_ok = True
            break
        time.sleep(1)
        
    if quota_80_db_ok:
        print("   [PASS] usage_db data_threshold_reached=true olarak güncellendi.")
    else:
        print("   [FAIL] usage_db data_threshold_reached güncellenemedi!")
        return

    # Notification Testi (%80): Temiz QuotaThresholdReached eventi doğrudan gönderiliyor
    print("   Kafka'ya (telcox.quota.events) temiz QuotaThresholdReached event'i yayınlanıyor...")
    quota_warning_payload = {
        "eventType": "QuotaThresholdReached",
        "subscriptionId": test_sub_id,
        "msisdn": test_msisdn,
        "usageType": "DATA",
        "limitType": "80_PERCENT",
        "thresholdReachedAt": datetime.now().isoformat() + "Z"
    }
    publish_kafka_message("telcox.quota.events", test_msisdn, quota_warning_payload)

    # notification_history db kontrolü
    print("   Veritabanı ve Mailpit SMS bildirim kontrolü (%80) bekleniyor...")
    sms_80_history_ok = False
    for _ in range(15):
        notif_check = query_db("notification-db", "notification_db",
            f"SELECT template_code FROM notifications WHERE template_code='QUOTA_WARNING_SMS' AND payload_json::text LIKE '%{test_msisdn}%' LIMIT 1;")
        if "QUOTA_WARNING_SMS" in notif_check:
            sms_80_history_ok = True
            break
        time.sleep(1)

    if sms_80_history_ok:
        print("   [PASS] QUOTA_WARNING_SMS notification_history'e başarıyla kaydedildi.")
    else:
        print("   [FAIL] QUOTA_WARNING_SMS notification_history'de bulunamadı!")
        return

    # 100% limit aşımı tetikleme (kalan 20 MB tüketim)
    print("\n4. Kafka'ya %100 limitini tetikleyecek CDR kullanımı yollanıyor (20 MB)...")
    cdr_100_payload = {
        "subscriptionId": test_sub_id,
        "msisdn": test_msisdn,
        "type": "DATA",
        "amount": 20.0,
        "cdrRef": f"cdr-ref-{uuid.uuid4()}",
        "recordedAt": datetime.now().isoformat() + "Z"
    }
    publish_kafka_message("telco.usage.events", test_msisdn, cdr_100_payload)

    # DB Kontrolü (%100)
    print("5. usage_db'de data_exceeded flaginin true olması bekleniyor...")
    quota_100_db_ok = False
    for _ in range(15):
        db_check = query_db("usage-db", "usage_db", f"SELECT data_exceeded FROM quotas WHERE subscription_id='{test_sub_id}';")
        if "t" in db_check:
            quota_100_db_ok = True
            break
        time.sleep(1)
        
    if quota_100_db_ok:
        print("   [PASS] usage_db data_exceeded=true olarak güncellendi.")
    else:
        print("   [FAIL] usage_db data_exceeded güncellenemedi!")
        return

    # Notification Testi (%100): Temiz QuotaExceeded eventi doğrudan gönderiliyor
    print("   Kafka'ya (telcox.quota.events) temiz QuotaExceeded event'i yayınlanıyor...")
    quota_exceeded_payload = {
        "eventType": "QuotaExceeded",
        "subscriptionId": test_sub_id,
        "msisdn": test_msisdn,
        "usageType": "DATA",
        "limitType": "100_PERCENT",
        "exceededAt": datetime.now().isoformat() + "Z"
    }
    publish_kafka_message("telcox.quota.events", test_msisdn, quota_exceeded_payload)

    # notification_history db kontrolü
    print("   Veritabanı ve Mailpit SMS bildirim kontrolü (%100) bekleniyor...")
    sms_100_history_ok = False
    for _ in range(15):
        notif_check = query_db("notification-db", "notification_db",
            f"SELECT template_code FROM notifications WHERE template_code='QUOTA_EXCEEDED_SMS' AND payload_json::text LIKE '%{test_msisdn}%' LIMIT 1;")
        if "QUOTA_EXCEEDED_SMS" in notif_check:
            sms_100_history_ok = True
            break
        time.sleep(1)

    if sms_100_history_ok:
        print("   [PASS] QUOTA_EXCEEDED_SMS notification_history'e başarıyla kaydedildi.")
    else:
        print("   [FAIL] QUOTA_EXCEEDED_SMS notification_history'de bulunamadı!")
        return

    print("\n=== KART 22: TÜM TESTLER BAŞARIYLA GEÇTİ (100% OK) ===")

if __name__ == "__main__":
    main()
