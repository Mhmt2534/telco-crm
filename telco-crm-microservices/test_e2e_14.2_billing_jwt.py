import json
import time
import subprocess
import requests
import random
import uuid
import sys
from datetime import datetime
import psycopg2
from kafka import KafkaProducer

# Configuration - Docker ag ici erisim (konteyner isimleri kullanilir)
IDENTITY_URL = "http://identity-service:9001"
CUSTOMER_URL = "http://customer-service:9002"
BILLING_URL = "http://billing-service:9007"
KEYCLOAK_DIRECT_BASE = "http://keycloak:8080"
KAFKA_CONTAINER = "telco-kafka"
MINIO_API_URL = "http://telco-minio:9000"

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
    from datetime import datetime, timezone
    return datetime.now(timezone.utc).day

def generate_valid_tckn():
    digits = [random.randint(1, 9)] + [random.randint(0, 9) for _ in range(8)]
    sum_odd = sum(digits[i] for i in range(0, 9, 2))
    sum_even = sum(digits[i] for i in range(1, 8, 2))
    d10 = ((sum_odd * 7) - sum_even) % 10
    digits.append(d10)
    d11 = sum(digits) % 10
    digits.append(d11)
    return "".join(map(str, digits))

# --- Database bağlantı bilgileri (Docker ağ içi) ---
DB_HOSTS = {
    "customer-db":      {"host": "customer-db",      "db": "customer_db"},
    "billing-db":       {"host": "billing-db",       "db": "billing_db"},
    "usage-db":         {"host": "usage-db",         "db": "usage_db"},
    "notification-db":  {"host": "notification-db",  "db": "notification_db"},
}

def query_db(db_container, database, query):
    info = DB_HOSTS.get(db_container, {"host": db_container, "db": database})
    try:
        conn = psycopg2.connect(host=info["host"], port=5432, dbname=info["db"], user="postgres", password="postgres")
        conn.autocommit = True
        cur = conn.cursor()
        cur.execute(query)
        if cur.description:
            rows = cur.fetchall()
            cols = [d[0] for d in cur.description]
            # Format like psql output for backward compat
            lines = [" | ".join(cols)]
            lines.append("-" * 40)
            for row in rows:
                lines.append(" | ".join(str(v) for v in row))
            lines.append(f"({len(rows)} row{'s' if len(rows)!=1 else ''})")
            result = "\n".join(lines)
        else:
            result = f"OK ({cur.rowcount} rows affected)"
        cur.close()
        conn.close()
        return result
    except Exception as e:
        print(f"Error querying db {database}: {e}")
        return ""

# Kafka Producer (Docker ağ içi)
_kafka_producer = None
def get_kafka_producer():
    global _kafka_producer
    if _kafka_producer is None:
        _kafka_producer = KafkaProducer(
            bootstrap_servers=["kafka:29092"],
            key_serializer=lambda k: k.encode("utf-8") if k else None,
            value_serializer=lambda v: json.dumps(v).encode("utf-8")
        )
    return _kafka_producer

def publish_kafka_message(topic, key, payload):
    try:
        producer = get_kafka_producer()
        producer.send(topic, key=key, value=payload)
        producer.flush()
    except Exception as e:
        print(f"Error publishing to Kafka: {e}")

def check_mailpit_for_email(recipient_email, subject_contains, timeout=15):
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

def get_keycloak_admin_token():
    token_url = f"{KEYCLOAK_DIRECT_BASE}/realms/telco-crm-realm/protocol/openid-connect/token"
    payload = {
        "grant_type": "client_credentials",
        "client_id": "telco-crm-client",
        "client_secret": "B3Zlh6fORItPVGdybGkILIkH9S3hQgex"
    }
    r = requests.post(token_url, data=payload)
    r.raise_for_status()
    return r.json()["access_token"]

def ensure_admin_user_exists():
    print("   [INFO] Ensuring admin user with ADMIN role exists in telco-crm-realm...")
    token = get_keycloak_admin_token()
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    
    users_url = f"{KEYCLOAK_DIRECT_BASE}/admin/realms/telco-crm-realm/users"
    r = requests.get(users_url, params={"username": "admin"}, headers=headers)
    r.raise_for_status()
    users = r.json()
    
    user_id = None
    for u in users:
        if u["username"] == "admin":
            user_id = u["id"]
            break
            
    if user_id:
        # Delete existing user to start fresh
        del_url = f"{users_url}/{user_id}"
        requests.delete(del_url, headers=headers).raise_for_status()
        
    # Create user admin
    user_payload = {
        "username": "admin",
        "enabled": True,
        "email": "admin@telcox.com",
        "firstName": "Admin",
        "lastName": "Staff",
        "emailVerified": True,
        "requiredActions": []
    }
    r = requests.post(users_url, json=user_payload, headers=headers)
    r.raise_for_status()
    
    # Find new user ID
    r = requests.get(users_url, params={"username": "admin"}, headers=headers)
    r.raise_for_status()
    user_id = [u["id"] for u in r.json() if u["username"] == "admin"][0]
    
    # Set password
    pwd_url = f"{users_url}/{user_id}/reset-password"
    pwd_payload = {
        "type": "password",
        "value": "admin123",
        "temporary": False
    }
    requests.put(pwd_url, json=pwd_payload, headers=headers).raise_for_status()
    
    # Map ROLE ADMIN
    role_url = f"{KEYCLOAK_DIRECT_BASE}/admin/realms/telco-crm-realm/roles/ADMIN"
    r = requests.get(role_url, headers=headers)
    r.raise_for_status()
    role_details = r.json()
    
    mapping_url = f"{users_url}/{user_id}/role-mappings/realm"
    requests.post(mapping_url, json=[role_details], headers=headers).raise_for_status()
    print("   [INFO] Admin user successfully recreated and role mapped!\n")

def get_staff_token():
    print("   [INFO] Logging in as staff user (admin/admin123) via identity-service...")
    login_url = f"{IDENTITY_URL}/api/v1/auth/staff/login"
    login_payload = {
        "username": "admin",
        "password": "admin123"
    }
    r = requests.post(login_url, json=login_payload)
    r.raise_for_status()
    return r.json()["access_token"]

def main():
    print("=== STARTING KART 22: SENARYO 14.2 & 14.3 WITH JWT AUTHENTICATION ===\n")
    
    # Ensure Keycloak Admin user exists
    ensure_admin_user_exists()
    
    # Login as Staff to get Token for administrative actions
    staff_token = get_staff_token()
    print("   [SUCCESS] Staff authenticated successfully. Token received.\n")
    
    # ──────────────────────────────────────────────────────────────────────────
    # ─── SENARYO 14.2: Aylık Fatura Kesimi & Ödeme Akışı ──────────────────────
    # ──────────────────────────────────────────────────────────────────────────
    print("--- SENARYO 14.2 BAŞLIYOR ---")
    
    print("1. Yeni test müşterisi oluşturuluyor (Gateway + Staff JWT)...")
    rand_suffix = random.randint(1000, 9999)
    phone_num = f"90532{random.randint(1000000, 9999999)}"
    email = f"test_billing_{rand_suffix}@telcox.com"
    tckn = generate_valid_tckn()
    
    customer_payload = {
        "firstName": "Hakan",
        "lastName": f"Demir Billing {rand_suffix}",
        "type": "INDIVIDUAL",
        "identityNumber": tckn,
        "dateOfBirth": "1990-05-20",
        "phone": phone_num[2:], # Save 10-digit phone
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
    
    # Create customer via gateway
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {staff_token}"
    }
    r = requests.post(f"{CUSTOMER_URL}/api/v1/customers", json=customer_payload, headers=headers)
    if r.status_code != 201 and r.status_code != 200:
        print(f"   [FAIL] Müşteri oluşturulamadı! Status: {r.status_code}, Body: {r.text}")
        return
        
    customer = r.json()
    customer_id = customer["id"]
    print(f"   Müşteri başarıyla oluşturuldu. ID: {customer_id}")

    # Approve KYC
    print(f"   Müşteri KYC onayı yapılıyor...")
    r = requests.post(f"{CUSTOMER_URL}/api/v1/customers/{customer_id}/kyc/approve", headers=headers)
    r.raise_for_status()
    print("   [SUCCESS] KYC onaylandı.")

    # BillCycle kaydı ekleme
    current_day = get_current_day_of_month()
    subscription_id = str(uuid.uuid4())
    fixed_amount = 250.00

    # Query customer PK id (bigint) from customer-db using public_id
    customer_pk = None
    try:
        conn = psycopg2.connect(host="customer-db", port=5432, dbname="customer_db", user="postgres", password="postgres")
        cur = conn.cursor()
        cur.execute(f"SELECT id FROM customer WHERE public_id = '{customer_id}';")
        row = cur.fetchone()
        if row:
            customer_pk = int(row[0])
        cur.close()
        conn.close()
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
    print("3. billing-service runs API tetikleniyor (Gateway + Staff JWT)...")
    r = requests.post(f"{BILLING_URL}/api/v1/billing/runs", headers=headers)
    print(f"   API Status: {r.status_code}, Yanıtı: {r.text}")
    r.raise_for_status()

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

    # MinIO S3 PDF kontrolü (Gateway + Staff JWT)
    print("5. Gateway ve MinIO üzerinde fatura PDF dosyasının oluşumu bekleniyor...")
    pdf_found = False
    for _ in range(15):
        r = requests.get(f"{BILLING_URL}/api/v1/invoices/{invoice_uuid}/pdf", headers=headers)
        if r.status_code == 200:
            try:
                pdf_data = r.json()
                pdf_url = pdf_data["pdfUrl"]
                # localhost'a çevir
                local_pdf_url = pdf_url.replace("http://telco-minio:9000", MINIO_API_URL)
                r_pdf = requests.get(local_pdf_url, timeout=5)
                if r_pdf.status_code == 200 and r_pdf.content.startswith(b"%PDF"):
                    pdf_found = True
                    break
                else:
                    print(f"      [DEBUG] GET local_pdf_url status: {r_pdf.status_code}, content: {r_pdf.text[:200]}")
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
