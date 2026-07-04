import urllib.request
import urllib.error
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

def main():
    base_url = "http://localhost:8080/api/v1"
    
    print("=== STARTING KART 12 E2E TEST ===")
    
    rand_suffix = random.randint(1000, 9999)
    phone = f"90555{random.randint(1000000, 9999999)}"
    tckn = generate_valid_tckn()
    email = f"test.user{rand_suffix}@example.com"
    
    # 1. Customer registration
    print("1. Sending customer registration request to POST /api/v1/customers...")
    customer_payload = {
        "type": "INDIVIDUAL",
        "firstName": "John",
        "lastName": f"Doe {rand_suffix}",
        "identityNumber": tckn,
        "dateOfBirth": "1990-01-01",
        "phone": phone,
        "email": email,
        "addresses": []
    }
    print(f"   [REQUEST PAYLOAD] {json.dumps(customer_payload, indent=2)}")
    
    req = urllib.request.Request(f"{base_url}/customers", data=json.dumps(customer_payload).encode('utf-8'), headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode('utf-8')
            print(f"   [RESPONSE BODY] {res_body}")
            customer_id = json.loads(res_body).get("id")
            if not customer_id:
                print("   [FAILED] Customer ID not found in response")
                sys.exit(1)
            print(f"   [SUCCESS] Customer created successfully with ID: {customer_id}\n")
    except urllib.error.HTTPError as e:
        print(f"   [FAILED] Customer creation HTTP error: {e.code} {e.reason}")
        print(f"            Response body: {e.read().decode('utf-8')}")
        sys.exit(1)

    # 2. KYC Approval
    print(f"2. Sending KYC approval request to POST /api/v1/customers/{customer_id}/kyc/approve...")
    req_kyc = urllib.request.Request(
        f"{base_url}/customers/{customer_id}/kyc/approve",
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    
    try:
        with urllib.request.urlopen(req_kyc) as response:
            res_body = response.read().decode('utf-8')
            print(f"   [RESPONSE BODY] {res_body if res_body else '<Empty Success Response>'}")
            print("   [SUCCESS] KYC approved successfully.\n")
    except urllib.error.HTTPError as e:
        print(f"   [FAILED] KYC approval failed: HTTP Error {e.code}: {e.reason}")
        print(f"            Response body: {e.read().decode('utf-8')}")
        sys.exit(1)

    # 3. Create Order
    order_payload = {
        "customerId": customer_id,
        "items": [
            {
                "productCode": "TRF-TEST",
                "productType": "TARIFF",
                "quantity": 1,
                "unitPrice": 199.99
            }
        ]
    }
    
    req_order = urllib.request.Request(
        f"{base_url}/orders",
        data=json.dumps(order_payload).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Idempotency-Key": "test-idempotency-123"
        },
        method="POST"
    )
    
    try:
        print("3. Sending order creation request to POST /api/v1/orders...")
        with urllib.request.urlopen(req_order) as response:
            order_res = json.loads(response.read().decode("utf-8"))
            order_id = order_res["id"]
            order_status = order_res["status"]
            print(f"   [SUCCESS] Order created successfully with ID: {order_id}")
            print(f"   [VERIFY] Order status: {order_status}")
            
            if order_status != "PENDING_PAYMENT":
                print(f"   [FAILED] Expected status 'PENDING_PAYMENT', but got '{order_status}'")
                sys.exit(1)
            else:
                print("   [SUCCESS] Order status matches 'PENDING_PAYMENT' acceptance criteria!")
    except Exception as e:
        print(f"   [FAILED] Order creation failed: {e}")
        sys.exit(1)

    # Wait for database write
    time.sleep(2)

    # 4. Verify Debezium Outbox Table
    print("4. Querying Debezium outbox table (outbox_event) in order-db container...")
    try:
        # Run query inside the Postgres container
        cmd = [
            "docker", "exec", "-i", "order-db", 
            "psql", "-U", "postgres", "-d", "order_db", 
            "-c", f"SELECT id, aggregate_type, aggregate_id, event_type, payload FROM outbox_event WHERE aggregate_id = '{order_id}';"
        ]
        
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        print("=== Database Query Output ===")
        print(result.stdout)
        print("=============================")
        
        if str(order_id) in result.stdout and "OrderCreated" in result.stdout:
            print("   [SUCCESS] Verified that order record successfully landed in Debezium outbox_event table!")
        else:
            print("   [FAILED] Outbox event for the order could not be verified in outbox_event table.")
            sys.exit(1)
            
    except Exception as e:
        print(f"   [FAILED] Database query failed: {e}")
        sys.exit(1)

    # 5. Create Subscription
    print("5. Sending subscription creation request to POST /api/v1/subscriptions...")
    sub_payload = {
        "customerId": customer_id,
        "tariffCode": "TRF-TEST"
    }
    print(f"   [REQUEST PAYLOAD] {json.dumps(sub_payload, indent=2)}")
    
    sub_req = urllib.request.Request(
        f"{base_url}/subscriptions",
        data=json.dumps(sub_payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    
    try:
        with urllib.request.urlopen(sub_req) as response:
            sub_res_body = response.read().decode("utf-8")
            print(f"   [RESPONSE BODY] {json.dumps(json.loads(sub_res_body), indent=2)}")
            sub_data = json.loads(sub_res_body)
            sub_id = sub_data.get("id")
            sub_status = sub_data.get("status")
            msisdn = sub_data.get("msisdn")
            
            print(f"   [SUCCESS] Subscription created successfully with ID: {sub_id}")
            print(f"   [VERIFY] Subscription MSISDN: {msisdn}")
            print(f"   [VERIFY] Subscription Status: {sub_status}")
            
            if sub_status == "ACTIVE":
                print("   [SUCCESS] Subscription status matches 'ACTIVE' acceptance criteria!\n")
            else:
                print(f"   [FAILED] Expected ACTIVE, got {sub_status}")
                sys.exit(1)
    except urllib.error.HTTPError as e:
        print(f"   [FAILED] Subscription creation failed: HTTP Error {e.code}: {e.reason}")
        print(f"            Response body: {e.read().decode('utf-8')}")
        sys.exit(1)

    print("=== KART 12 E2E TEST PASSED ===")

if __name__ == "__main__":
    main()
