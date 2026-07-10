import subprocess
import time
import requests
import json
import random
import uuid

GATEWAY_URL = "http://localhost:8080"
MAILPIT_API = "http://localhost:8025/api/v1/messages"
CUSTOMER_SERVICE_URL = f"{GATEWAY_URL}/api/v1/customers"
SUBSCRIPTION_URL = f"{GATEWAY_URL}/api/v1/subscriptions"
KAFKA_CONTAINER = "telco-kafka"
KAFKA_BOOTSTRAP = "kafka:29092"


def publish_kafka_message(topic, message_key, payload_dict, headers=None):
    """Publishes a JSON message to a Kafka topic via docker exec."""
    payload_str = json.dumps(payload_dict)
    input_data = f"{message_key}:{payload_str}"
    cmd = [
        "docker", "exec", "-i", KAFKA_CONTAINER,
        "/opt/bitnami/kafka/bin/kafka-console-producer.sh",
        "--bootstrap-server", KAFKA_BOOTSTRAP,
        "--topic", topic,
        "--property", "parse.key=true",
        "--property", "key.separator=:"
    ]
    result = subprocess.run(cmd, input=input_data, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"   [ERROR] Kafka publish failed: {result.stderr}")
        return False
    return True


def get_mailpit_messages():
    """Fetches all messages from Mailpit API."""
    try:
        resp = requests.get(MAILPIT_API, timeout=5)
        if resp.status_code == 200:
            return resp.json().get("messages", [])
    except Exception as e:
        print(f"   [ERROR] Mailpit API error: {e}")
    return []


def clear_mailpit():
    """Deletes all messages in Mailpit."""
    try:
        requests.delete("http://localhost:8025/api/v1/messages", timeout=5)
    except:
        pass


def query_db(container, db_name, query):
    cmd = ["docker", "exec", "-i", container,
           "psql", "-U", "postgres", "-d", db_name, "-c", query]
    result = subprocess.run(cmd, capture_output=True, text=True)
    return result.stdout


def main():
    print("=== KART 21.5: Notification Domain Event Consumer E2E Test ===\n")

    # Mailpit'i temizle
    clear_mailpit()
    time.sleep(1)

    sub_id = str(uuid.uuid4())
    msisdn = f"905{random.randint(100000000, 999999999)}"
    customer_name = "Ahmet Test"
    email = f"test_{random.randint(1000,9999)}@telcox.com"

    # ─── TEST 1: SubscriptionActivated → WELCOME_SMS ──────────────────────────
    print("Test 1: SubscriptionActivated -> WELCOME_SMS bekleniyor...")
    event1 = {
        "eventType": "SubscriptionActivated",
        "subscriptionId": sub_id,
        "customerId": str(uuid.uuid4()),
        "msisdn": msisdn,
        "customerName": customer_name,
        "email": email,
        "tariffName": "Standart Paket"
    }
    publish_kafka_message("telcox.Subscription.events", msisdn, event1)

    # notification_history tablosunda SMS kaydı var mı?
    sms_found = False
    for _ in range(10):
        sms_check = query_db("notification-db", "notification_db",
            "SELECT template_code, channel, status FROM notifications WHERE channel='SMS' AND template_code='WELCOME_SMS' LIMIT 1;")
        if "WELCOME_SMS" in sms_check:
            sms_found = True
            break
        time.sleep(1)

    if sms_found:
        print(f"   [PASS] WELCOME_SMS notification_history'e kaydedildi.")
    else:
        print(f"   [FAIL] WELCOME_SMS notification_history'de bulunamadi!")
        print(f"   DB output: {sms_check}")

    # ─── TEST 2: InvoiceGenerated → INVOICE_EMAIL ────────────────────────────
    print("\nTest 2: InvoiceGenerated -> INVOICE_EMAIL bekleniyor...")
    invoice_id = str(uuid.uuid4())
    event2 = {
        "eventType": "InvoiceGenerated",
        "invoiceId": invoice_id,
        "customerId": str(uuid.uuid4()),
        "subscriptionId": sub_id,
        "customerName": customer_name,
        "email": email,
        "amount": 149.99,
        "invoiceMonth": "Temmuz 2026",
        "pdfUrl": "http://localhost:9000/telcox-invoices/invoices/1/1.pdf"
    }
    publish_kafka_message("telcox.invoice.events", invoice_id, event2)

    # Mailpit'te email geldi mi?
    print("   Mailpit kontrol ediliyor (max 10 saniye)...")
    email_found = False
    for i in range(10):
        time.sleep(1)
        messages = get_mailpit_messages()
        if messages:
            for msg in messages:
                if email in str(msg):
                    email_found = True
                    break
        if email_found:
            break

    if email_found:
        print(f"   [PASS] Fatura e-postası Mailpit'te bulundu! Alıcı: {email}")
    else:
        print(f"   [FAIL] Fatura e-postası Mailpit'te bulunamadı.")

    # ─── TEST 3: QuotaThresholdReached → QUOTA_WARNING_SMS ────────────────────
    print("\nTest 3: QuotaThresholdReached -> QUOTA_WARNING_SMS bekleniyor...")
    event3 = {
        "eventType": "QuotaThresholdReached",
        "subscriptionId": sub_id,
        "msisdn": msisdn,
        "usageType": "DATA",
        "limitType": "80_PERCENT",
        "thresholdReachedAt": "2026-07-10T09:00:00Z"
    }
    publish_kafka_message("telcox.quota.events", msisdn, event3)

    time.sleep(4)

    quota_warn_check = query_db("notification-db", "notification_db",
        "SELECT template_code, channel, status FROM notifications WHERE template_code='QUOTA_WARNING_SMS' LIMIT 1;")
    if "QUOTA_WARNING_SMS" in quota_warn_check:
        print(f"   [PASS] QUOTA_WARNING_SMS notification_history'e kaydedildi.")
    else:
        print(f"   [FAIL] QUOTA_WARNING_SMS notification_history'de bulunamadi!")
        print(f"   DB output: {quota_warn_check}")

    # ─── TEST 4: QuotaExceeded → QUOTA_EXCEEDED_SMS ───────────────────────────
    print("\nTest 4: QuotaExceeded -> QUOTA_EXCEEDED_SMS bekleniyor...")
    event4 = {
        "eventType": "QuotaExceeded",
        "subscriptionId": sub_id,
        "msisdn": msisdn,
        "usageType": "DATA",
        "limitType": "100_PERCENT",
        "exceededAt": "2026-07-10T09:05:00Z"
    }
    publish_kafka_message("telcox.quota.events", msisdn, event4)

    time.sleep(4)

    quota_exc_check = query_db("notification-db", "notification_db",
        "SELECT template_code, channel, status FROM notifications WHERE template_code='QUOTA_EXCEEDED_SMS' LIMIT 1;")
    if "QUOTA_EXCEEDED_SMS" in quota_exc_check:
        print(f"   [PASS] QUOTA_EXCEEDED_SMS notification_history'e kaydedildi.")
    else:
        print(f"   [FAIL] QUOTA_EXCEEDED_SMS notification_history'de bulunamadi!")
        print(f"   DB output: {quota_exc_check}")

    # ─── TEST 5: InvoiceOverdue → INVOICE_OVERDUE_SMS ─────────────────────────
    print("\nTest 5: InvoiceOverdue -> INVOICE_OVERDUE_SMS bekleniyor...")
    event5 = {
        "eventType": "InvoiceOverdue",
        "invoiceId": str(uuid.uuid4()),
        "customerId": str(uuid.uuid4()),
        "customerName": customer_name,
        "msisdn": msisdn,
        "email": email,
        "amount": 149.99,
        "dueDate": "2026-07-01"
    }
    publish_kafka_message("telcox.invoice.events", str(uuid.uuid4()), event5)

    time.sleep(4)

    overdue_check = query_db("notification-db", "notification_db",
        "SELECT template_code, channel, status FROM notifications WHERE template_code='INVOICE_OVERDUE_SMS' LIMIT 1;")
    if "INVOICE_OVERDUE_SMS" in overdue_check:
        print(f"   [PASS] INVOICE_OVERDUE_SMS notification_history'e kaydedildi.")
    else:
        print(f"   [FAIL] INVOICE_OVERDUE_SMS notification_history'de bulunamadi!")
        print(f"   DB output: {overdue_check}")

    # ─── ÖZET ────────────────────────────────────────────────────────────────
    print("\n=== notification_history tablosu son durumu ===")
    final_check = query_db("notification-db", "notification_db",
        "SELECT template_code, channel, status FROM notifications ORDER BY created_at DESC LIMIT 10;")
    print(final_check)
    print("=== Test Tamamlandi ===")


if __name__ == "__main__":
    main()
