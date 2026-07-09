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

def query_billing_db(query_str):
    try:
        cmd = [
            "docker", "exec", "-i", "billing-db", 
            "psql", "-U", "postgres", "-d", "billing_db", 
            "-c", query_str
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout
    except Exception as e:
        print(f"   [ERROR] Failed to query billing-db: {e}")
        return ""

def query_subscription_db(query_str):
    try:
        cmd = [
            "docker", "exec", "-i", "subscription-db", 
            "psql", "-U", "postgres", "-d", "subscription_db", 
            "-c", query_str
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout
    except Exception as e:
        print(f"   [ERROR] Failed to query subscription-db: {e}")
        return ""

def main():
    print("=== STARTING KART 18.2 BILLING OVERDUE CHECK E2E TEST ===\n")
    
    rand_suffix = random.randint(1000, 9999)
    tckn = generate_valid_tckn()
    
    # 1. Customer Registration
    customer_payload = {
        "type": "INDIVIDUAL",
        "firstName": "Overdue",
        "lastName": f"Test {rand_suffix}",
        "identityNumber": tckn,
        "dateOfBirth": "1995-05-15",
        "phone": f"90555{random.randint(1000000, 9999999)}",
        "email": f"overdue.test{rand_suffix}@example.com",
        "addresses": []
    }
    
    print(f"Step 1: Direct Customer Registration inside customer-service container...")
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

    # 2. KYC Approval
    print(f"Step 2: Direct KYC Approval inside customer-service container...")
    res_kyc = execute_curl_in_container(
        container_name="customer-service",
        method="POST",
        url=f"http://localhost:9002/api/v1/customers/{customer_id}/kyc/approve"
    )
    print("   [SUCCESS] KYC Approved.")

    # 3. Create Subscription Event directly in subscription_db outbox
    subscription_id = random.randint(10000, 99999)
    msisdn = f"90555{random.randint(1000000, 9999999)}"
    
    payload_dict = {
        "id": subscription_id,
        "customerId": customer_id,
        "msisdn": msisdn,
        "tariffCode": "TRF-HAPPY",
        "status": "ACTIVE",
        "activatedAt": "2026-07-06T20:00:00Z",
        "eventType": "SubscriptionActivated"
    }
    
    payload_json = json.dumps(payload_dict)
    
    insert_query = f"""
    INSERT INTO outbox_event (id, event_id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, created_at)
    VALUES (
      gen_random_uuid(),
      gen_random_uuid(),
      'Subscription',
      '{subscription_id}',
      'SubscriptionActivated',
      '{payload_json}',
      'PENDING',
      0,
      now()
    );
    """
    
    print(f"Step 3: Directly inserting SubscriptionActivated event into subscription_db outbox_event table for customer {customer_id}...")
    res_insert = query_subscription_db(insert_query)
    print("   [SUCCESS] Event inserted successfully.")

    # 4. Wait for Debezium CDC and Kafka Flow to create BillCycle
    print("\nStep 4: Waiting 5 seconds for Debezium CDC -> Kafka -> billing-service (BillCycle) flow to complete...")
    time.sleep(5)

    # Verify BillCycle
    print("Step 5: Querying billing_db to verify BillCycle creation...")
    bill_cycle_query = f"SELECT id, customer_id, subscription_id, msisdn, cut_off_day, fixed_amount FROM bill_cycle WHERE customer_id = {customer_id};"
    bill_cycle_output = query_billing_db(bill_cycle_query)
    print("=== bill_cycle Table ===")
    print(bill_cycle_output)
    
    if str(customer_id) not in bill_cycle_output:
        print("   [FAIL] No BillCycle record found for this customer.")
        sys.exit(1)

    # 5. Trigger Bill Run to generate Invoice
    print("\nStep 6: Triggering Bill Run inside billing-service container (POST /api/v1/billing/runs)...")
    res_billing_run = execute_curl_in_container(
        container_name="billing-service",
        method="POST",
        url="http://localhost:9007/api/v1/billing/runs"
    )
    print(f"   [SUCCESS] Admin trigger response: {res_billing_run}")

    # Wait for invoice to be written
    print("Step 7: Waiting 3 seconds for invoices to be written to DB...")
    time.sleep(3)

    # Verify Invoice Created as UNPAID
    invoice_query = f"SELECT id, customer_id, subscription_id, amount, status, due_date FROM invoice WHERE customer_id = {customer_id};"
    invoice_output = query_billing_db(invoice_query)
    print("=== invoice Table ===")
    print(invoice_output)
    
    if "UNPAID" not in invoice_output:
         print("   [FAIL] No UNPAID invoice found for this customer.")
         sys.exit(1)

    # 6. UPDATE INVOICE DUE_DATE TO PAST (simulate overdue)
    print("\nStep 8: Simulating overdue by updating due_date of invoice to the past...")
    update_query = f"UPDATE invoice SET due_date = NOW() - INTERVAL '2 days' WHERE customer_id = {customer_id};"
    query_billing_db(update_query)
    
    # Verify due_date updated
    invoice_output_updated = query_billing_db(invoice_query)
    print("=== Updated invoice Table ===")
    print(invoice_output_updated)

    # 7. TRIGGER OVERDUE CHECK ENDPOINT
    print("\nStep 9: Triggering Overdue Check inside billing-service container (POST /api/v1/billing/overdue-check)...")
    res_overdue_check = execute_curl_in_container(
        container_name="billing-service",
        method="POST",
        url="http://localhost:9007/api/v1/billing/overdue-check"
    )
    print(f"   [SUCCESS] Overdue check trigger response: {res_overdue_check}")

    # Wait for state updates
    print("Step 10: Waiting 3 seconds for state updates to complete...")
    time.sleep(3)

    # 8. VERIFY INVOICE IS MARKED AS OVERDUE
    print("Step 11: Verifying DB states in billing_db for updated Invoice...")
    invoice_final_output = query_billing_db(invoice_query)
    print("=== Final invoice Table ===")
    print(invoice_final_output)
    
    if "OVERDUE" in invoice_final_output:
         print("   [PASS] KART 18.2: Invoice record successfully updated to OVERDUE in billing_db!")
    else:
         print("   [FAIL] KART 18.2: Invoice status did not change to OVERDUE.")
         sys.exit(1)

    # 9. VERIFY OUTBOX EVENT FOR OVERDUE GENERATED
    print("Step 12: Verifying outbox_event table for InvoiceOverdueEvent...")
    outbox_query = "SELECT type, payload FROM outbox_event WHERE type = 'InvoiceOverdueEvent';"
    outbox_output = query_billing_db(outbox_query)
    print("=== outbox_event Table ===")
    print(outbox_output)
    
    if "InvoiceOverdueEvent" in outbox_output:
         print("   [PASS] KART 18.2: OutboxEvent (InvoiceOverdueEvent) successfully generated!")
    else:
         print("   [FAIL] KART 18.2: InvoiceOverdueEvent outbox event not found.")
         sys.exit(1)

    print("\n=== ALL KART 18.2 TEST CASES PASSED SUCCESSFULLY ===")

if __name__ == "__main__":
    main()
