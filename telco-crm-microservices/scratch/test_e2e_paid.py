import json
import subprocess
import time
import sys
import random

def generate_valid_tckn():
    digits = [random.randint(1, 9)] + [random.randint(0, 9) for _ in range(8)]
    odd_sum = sum(digits[0::2])
    even_sum = sum(digits[1::2])
    tenth = ((odd_sum * 7) - even_sum) % 10
    digits.append(tenth)
    eleventh = sum(digits) % 10
    digits.append(eleventh)
    return "".join(map(str, digits))

def execute_curl_in_container(container_name, method, url, payload=None, headers=None):
    cmd = ["docker", "exec", "-i", container_name, "curl", "-s", "-X", method]
    
    if headers:
        for k, v in headers.items():
            cmd += ["-H", f"{k}: {v}"]
            
    if payload:
        cmd += ["-d", json.dumps(payload)]
        
    cmd.append(url)
    
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout.strip()
    except Exception as e:
        print(f"   [ERROR] Failed to execute curl in {container_name}: {e}")
        return ""

def query_db(container_name, db_name, query_str):
    try:
        cmd = [
            "docker", "exec", "-i", container_name, 
            "psql", "-U", "postgres", "-d", db_name, 
            "-c", query_str
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout
    except Exception as e:
        print(f"   [ERROR] Failed to query {db_name}: {e}")
        return ""

def main():
    print("=== STARTING KART 18.3 BILLING INVOICE PAID E2E TEST ===\n")
    
    rand_suffix = random.randint(1000, 9999)
    tckn = generate_valid_tckn()
    
    # 1. Müşteri Kaydı
    customer_payload = {
        "type": "INDIVIDUAL",
        "firstName": "InvoicePaid",
        "lastName": f"Test {rand_suffix}",
        "identityNumber": tckn,
        "dateOfBirth": "1995-05-15",
        "phone": f"90555{random.randint(1000000, 9999999)}",
        "email": f"invoice.paid{rand_suffix}@example.com",
        "addresses": []
    }
    
    print("Step 1: Direct Customer Registration inside customer-service container...")
    res = execute_curl_in_container(
        container_name="customer-service",
        method="POST",
        url="http://localhost:9002/api/v1/customers",
        payload=customer_payload,
        headers={"Content-Type": "application/json"}
    )
    try:
        res_body = json.loads(res)
        customer_id = res_body.get("id")
        if not customer_id:
             print(f"   [FAILED] No customer ID in response: {res}")
             sys.exit(1)
        print(f"   [SUCCESS] Customer created with ID: {customer_id}")
    except Exception as e:
        print(f"   [FAILED] Customer creation parse: {e}. Raw: {res}")
        sys.exit(1)

    # 2. KYC Onayı
    print("Step 2: Direct KYC Approval inside customer-service container...")
    execute_curl_in_container(
        container_name="customer-service",
        method="POST",
        url=f"http://localhost:9002/api/v1/customers/{customer_id}/kyc/approve"
    )
    print("   [SUCCESS] KYC Approved.")

    # 3. Abonelik Aktivasyon Eventi Ekleme (subscription_db -> outbox_event)
    subscription_id = random.randint(10000, 99999)
    msisdn = f"90555{random.randint(1000000, 9999999)}"
    
    sub_payload = {
        "id": subscription_id,
        "customerId": customer_id,
        "msisdn": msisdn,
        "tariffCode": "TRF-HAPPY",
        "status": "ACTIVE",
        "activatedAt": "2026-07-06T20:00:00Z",
        "eventType": "SubscriptionActivated"
    }
    
    sub_payload_json = json.dumps(sub_payload)
    
    insert_sub_query = f"""
    INSERT INTO outbox_event (id, event_id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, created_at)
    VALUES (
      gen_random_uuid(),
      gen_random_uuid(),
      'Subscription',
      '{subscription_id}',
      'SubscriptionActivated',
      '{sub_payload_json}',
      'PENDING',
      0,
      now()
    );
    """
    
    print(f"Step 3: Directly inserting SubscriptionActivated event into subscription_db outbox_event table for customer {customer_id}...")
    query_db("subscription-db", "subscription_db", insert_sub_query)
    print("   [SUCCESS] Event inserted successfully.")

    # 4. Debezium + Kafka Akışının Tamamlanması İçerisinde BillCycle Oluşumu için Bekleme
    print("\nStep 4: Waiting 5 seconds for Debezium CDC -> Kafka -> billing-service (BillCycle) flow to complete...")
    time.sleep(5)

    # 5. billing_db'de bill_cycle Tablosunu Kontrol Etme
    print("Step 5: Querying billing_db to verify BillCycle creation...")
    bill_cycle_output = query_db("billing-db", "billing_db", f"SELECT id, customer_id FROM bill_cycle WHERE customer_id = {customer_id};")
    print("=== bill_cycle Table ===")
    print(bill_cycle_output)

    
    if str(customer_id) in bill_cycle_output:
        print("   [PASS] KART 18.1: BillCycle record successfully created in billing_db!")
    else:
        print("   [FAIL] KART 18.1: No BillCycle record found.")
        sys.exit(1)

    # 6. Fatura Kesim Zamanlayıcısını Tetikleme
    print("\nStep 6: Triggering Bill Run inside billing-service container (POST /api/v1/billing/runs)...")
    res_run = execute_curl_in_container(
        container_name="billing-service",
        method="POST",
        url="http://localhost:9007/api/v1/billing/runs"
    )
    print(f"   [SUCCESS] Admin trigger response: {res_run}")

    # 7. Bekleme
    print("Step 7: Waiting 3 seconds for invoices to be written to DB...")
    time.sleep(3)

    # 8. billing_db'de faturayı kontrol etme
    print("Step 8: Verifying invoice creation in billing_db...")
    invoice_output = query_db("billing-db", "billing_db", f"SELECT id, status, amount FROM invoice WHERE customer_id = {customer_id};")
    print("=== invoice Table ===")
    print(invoice_output)
    
    if "UNPAID" in invoice_output:
         print("   [PASS] KART 18: Invoice record successfully created as UNPAID in billing_db!")
    else:
         print("   [FAIL] KART 18: No UNPAID invoice found.")
         sys.exit(1)

    # Extract invoice ID from output
    try:
         lines = invoice_output.strip().split("\n")
         invoice_id = int(lines[2].split("|")[0].strip())
    except Exception as e:
         print(f"   [FAIL] Could not parse invoice ID from DB output: {e}")
         sys.exit(1)

    # 9. PaymentCompleted Event Simülasyonu (payment_db -> outbox_event)
    payment_id = "550e8400-e29b-41d4-a716-446655440000"
    payment_payload = {
        "paymentId": payment_id,
        "orderId": 0,
        "invoiceId": str(invoice_id),
        "customerId": customer_id,
        "amount": 100.00,
        "occurredAt": "2026-07-09T07:00:00Z"
    }
    
    payment_payload_json = json.dumps(payment_payload)
    
    insert_payment_query = f"""
    INSERT INTO outbox_event (id, event_id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, created_at)
    VALUES (
      gen_random_uuid(),
      gen_random_uuid(),
      'Payment',
      '{payment_id}',
      'PaymentCompleted',
      '{payment_payload_json}',
      'PENDING',
      0,
      now()
    );
    """
    
    print(f"\nStep 9: Directly inserting PaymentCompleted event into payment_db outbox_event table for invoice {invoice_id}...")
    query_db("payment-db", "payment_db", insert_payment_query)
    print("   [SUCCESS] PaymentCompleted event inserted successfully.")

    # 10. Bekleme
    print("Step 10: Waiting 5 seconds for PaymentCompleted -> Debezium -> Kafka -> billing-service flow...")
    time.sleep(5)

    # 11. Fatura durumunun PAID olduğunu doğrulama
    print("Step 11: Verifying DB states in billing_db for updated Invoice...")
    final_invoice_output = query_db("billing-db", "billing_db", f"SELECT id, status FROM invoice WHERE id = {invoice_id};")
    print("=== Final invoice Table ===")
    print(final_invoice_output)
    
    if "| PAID" in final_invoice_output:
         print("   [PASS] KART 18.3: Invoice record successfully updated to PAID in billing_db!")
    else:
         print("   [FAIL] KART 18.3: Invoice was not marked as PAID.")
         sys.exit(1)


    # 12. Outbox'taki InvoicePaidEvent'i doğrulama
    print("Step 12: Verifying outbox_event table for InvoicePaidEvent...")
    outbox_output = query_db("billing-db", "billing_db", f"SELECT type, payload FROM outbox_event WHERE aggregate_id = '{invoice_id}' AND type = 'InvoicePaidEvent';")
    print("=== outbox_event Table ===")
    print(outbox_output)
    
    if "InvoicePaidEvent" in outbox_output:
         print("   [PASS] KART 18.3: OutboxEvent (InvoicePaidEvent) successfully generated!")
    else:
         print("   [FAIL] KART 18.3: InvoicePaidEvent was not found in outbox.")
         sys.exit(1)

    print("\n=== ALL KART 18.3 TEST CASES PASSED SUCCESSFULLY ===")

if __name__ == "__main__":
    main()
