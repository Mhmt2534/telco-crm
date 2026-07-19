import urllib.request
import urllib.error
import json
import subprocess
import time
import sys
import random

# Base URL pointing to the API Gateway
GATEWAY_BASE = "http://localhost:8080"
KEYCLOAK_DIRECT_BASE = "http://localhost:9011"

def generate_valid_tckn():
    digits = [random.randint(1, 9)] + [random.randint(0, 9) for _ in range(8)]
    odd_sum = sum(digits[0::2])
    even_sum = sum(digits[1::2])
    tenth = ((odd_sum * 7) - even_sum) % 10
    digits.append(tenth)
    eleventh = sum(digits) % 10
    digits.append(eleventh)
    return "".join(map(str, digits))

def get_keycloak_admin_token():
    token_url = f"{KEYCLOAK_DIRECT_BASE}/realms/telco-crm-realm/protocol/openid-connect/token"
    payload = "grant_type=client_credentials&client_id=telco-crm-client&client_secret=B3Zlh6fORItPVGdybGkILIkH9S3hQgex"
    req = urllib.request.Request(
        token_url,
        data=payload.encode("utf-8"),
        headers={"Content-Type": "application/x-www-form-urlencoded"}
    )
    with urllib.request.urlopen(req) as response:
        return json.loads(response.read().decode("utf-8"))["access_token"]

def ensure_admin_user_exists():
    print("   [INFO] Ensuring admin user with ADMIN role exists in telco-crm-realm...")
    token = get_keycloak_admin_token()
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    
    users_url = f"{KEYCLOAK_DIRECT_BASE}/admin/realms/telco-crm-realm/users"
    req_list = urllib.request.Request(users_url + "?username=admin", headers=headers)
    with urllib.request.urlopen(req_list) as response:
        users = json.loads(response.read().decode("utf-8"))
        for u in users:
            if u["username"] == "admin":
                print("   [INFO] Admin user exists, deleting and recreating to ensure fresh settings...")
                req_del = urllib.request.Request(f"{users_url}/{u['id']}", headers=headers, method="DELETE")
                urllib.request.urlopen(req_del)
                
    # Create fresh user
    create_payload = {
        "username": "admin",
        "email": "admin@telcox.com",
        "firstName": "Admin",
        "lastName": "User",
        "enabled": True,
        "emailVerified": True,
        "requiredActions": []
    }
    req_create = urllib.request.Request(
        users_url,
        data=json.dumps(create_payload).encode("utf-8"),
        headers=headers,
        method="POST"
    )
    urllib.request.urlopen(req_create)
    
    # Get ID
    req_get = urllib.request.Request(users_url + "?username=admin", headers=headers)
    with urllib.request.urlopen(req_get) as response:
        user_id = json.loads(response.read().decode("utf-8"))[0]["id"]
        
    # Reset Password
    pwd_url = f"{users_url}/{user_id}/reset-password"
    pwd_payload = {
        "type": "password",
        "value": "admin123",
        "temporary": False
    }
    req_pwd = urllib.request.Request(pwd_url, data=json.dumps(pwd_payload).encode("utf-8"), headers=headers, method="PUT")
    urllib.request.urlopen(req_pwd)
    
    # Map ROLE ADMIN
    role_url = f"{KEYCLOAK_DIRECT_BASE}/admin/realms/telco-crm-realm/roles/ADMIN"
    req_role = urllib.request.Request(role_url, headers=headers)
    with urllib.request.urlopen(req_role) as response:
        role_details = json.loads(response.read().decode("utf-8"))
        
    mapping_url = f"{users_url}/{user_id}/role-mappings/realm"
    req_map = urllib.request.Request(mapping_url, data=json.dumps([role_details]).encode("utf-8"), headers=headers, method="POST")
    urllib.request.urlopen(req_map)
    print("   [INFO] Admin user successfully recreated and role mapped!\n")

def get_staff_token():
    print("   [INFO] Logging in as staff user (admin/admin123) via API Gateway...")
    login_url = f"{GATEWAY_BASE}/api/v1/auth/staff/login"
    login_payload = {
        "username": "admin",
        "password": "admin123"
    }
    req = urllib.request.Request(
        login_url,
        data=json.dumps(login_payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    with urllib.request.urlopen(req) as response:
        res = json.loads(response.read().decode("utf-8"))
        return res["access_token"]

def main():
    print("=== STARTING KART 14.1 E2E ONBOARDING WITH JWT TEST ===")
    
    # Ensure Keycloak Admin user exists
    ensure_admin_user_exists()
    
    # Login as Staff
    staff_token = get_staff_token()
    print("   [SUCCESS] Staff authenticated successfully. Token received.\n")
    
    rand_suffix = random.randint(1000, 9999)
    local_phone = f"555{random.randint(1000000, 9999999)}"
    phone_with_prefix = f"90{local_phone}"
    tckn = generate_valid_tckn()
    email = f"test.jwt{rand_suffix}@example.com"
    
    # 1. Customer registration (Requires Staff Role/Token)
    print("1. Sending customer registration request to POST /api/v1/customers via Gateway...")
    customer_payload = {
        "type": "INDIVIDUAL",
        "firstName": "John",
        "lastName": f"Doe JWT {rand_suffix}",
        "identityNumber": tckn,
        "dateOfBirth": "1990-01-01",
        "phone": local_phone, # Save as 10-digit in DB
        "email": email,
        "addresses": []
    }
    
    req = urllib.request.Request(
        f"{GATEWAY_BASE}/api/v1/customers",
        data=json.dumps(customer_payload).encode('utf-8'),
        headers={
            'Content-Type': 'application/json',
            'Authorization': f'Bearer {staff_token}'
        }
    )
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
 
    # 2. KYC Approval (Requires Staff Role/Token)
    print(f"2. Sending KYC approval request to POST /api/v1/customers/{customer_id}/kyc/approve via Gateway...")
    req_kyc = urllib.request.Request(
        f"{GATEWAY_BASE}/api/v1/customers/{customer_id}/kyc/approve",
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {staff_token}"
        },
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

    # 3. Log in as the Customer using OTP flow via Gateway
    print("3. Logging in as the new Customer...")
    # Step 3.1: Request OTP
    print(f"   3.1 Requesting OTP for phone {phone_with_prefix} via Gateway...")
    otp_req = urllib.request.Request(
        f"{GATEWAY_BASE}/api/v1/auth/customer/request-otp",
        data=json.dumps({"phone": phone_with_prefix}).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    try:
        urllib.request.urlopen(otp_req)
        print("   [SUCCESS] OTP requested.")
    except urllib.error.HTTPError as e:
        print(f"   [FAILED] OTP request failed: {e.code} {e.reason}")
        print(f"            Response body: {e.read().decode('utf-8')}")
        sys.exit(1)

    # Step 3.2: Query Redis directly for the generated OTP code
    print("   3.2 Fetching OTP from Redis...")
    otp_code = None
    try:
        cmd = ["docker", "exec", "telco-redis", "redis-cli", "GET", f"otp:{phone_with_prefix}"]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        otp_code = result.stdout.strip()
        print(f"   [SUCCESS] Fetched OTP from Redis: {otp_code}")
    except Exception as e:
        print(f"   [FAILED] Failed to query Redis for OTP: {e}")
        sys.exit(1)

    # Step 3.3: Verify OTP and get Customer JWT
    print("   3.3 Verifying OTP to obtain Customer access token...")
    verify_payload = {
        "phone": phone_with_prefix,
        "otp": otp_code
    }
    verify_req = urllib.request.Request(
        f"{GATEWAY_BASE}/api/v1/auth/customer/verify-otp",
        data=json.dumps(verify_payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    try:
        with urllib.request.urlopen(verify_req) as response:
            verify_res = json.loads(response.read().decode("utf-8"))
            customer_token = verify_res["access_token"]
            print("   [SUCCESS] Customer authenticated successfully! JWT token received.\n")
    except urllib.error.HTTPError as e:
        print(f"   [FAILED] Customer OTP verification failed: {e.code} {e.reason}")
        print(f"            Response body: {e.read().decode('utf-8')}")
        sys.exit(1)

    # 3.4: Get dynamic active tariff from product-catalog-service via Gateway
    print("3.4 Querying active tariffs from product-catalog-service via Gateway...")
    tariffs_req = urllib.request.Request(
        f"{GATEWAY_BASE}/api/v1/tariffs",
        headers={"Authorization": f"Bearer {customer_token}"}
    )
    try:
        with urllib.request.urlopen(tariffs_req) as response:
            tariffs_data = json.loads(response.read().decode("utf-8"))
            tariffs_list = tariffs_data.get("content", [])
            if not tariffs_list:
                print("   [FAILED] No active tariffs returned from catalog-service")
                sys.exit(1)
            
            # Use POSTPAID_50 or fallback to the first active tariff
            selected_tariff = None
            for t in tariffs_list:
                if t.get("code") == "POSTPAID_50":
                    selected_tariff = t
                    break
            if not selected_tariff:
                selected_tariff = tariffs_list[0]
                
            tariff_id = selected_tariff["id"]
            tariff_code = selected_tariff["code"]
            tariff_fee = selected_tariff["monthlyFee"]
            print(f"   [SUCCESS] Selected tariff: {tariff_code} with ID {tariff_id} (Fee: {tariff_fee})")
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8") if e.fp else ""
        print(f"   [FAILED] Failed to fetch active tariffs: {e}")
        print(f"   [RESPONSE BODY] {body}")
        sys.exit(1)
    except Exception as e:
        print(f"   [FAILED] Failed to fetch active tariffs: {e}")
        sys.exit(1)

    # 4. Create Order (Using Customer JWT! - Gateway UserContextRelayFilter injects X-User-Id)
    order_payload = {
        "customerId": customer_id,
        "items": [
            {
                "productId": tariff_id,
                "productType": "TARIFF",
                "quantity": 1,
                "unitPrice": tariff_fee
            }
        ]
    }
    
    req_order = urllib.request.Request(
        f"{GATEWAY_BASE}/api/v1/orders",
        data=json.dumps(order_payload).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Idempotency-Key": f"test-idempotency-{rand_suffix}",
            "Authorization": f"Bearer {customer_token}"
        },
        method="POST"
    )
    
    try:
        print("4. Sending order creation request to POST /api/v1/orders via Gateway...")
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
 
    # 5. Verify Debezium Outbox Table
    print("5. Querying Debezium outbox table (outbox_event) in order-db container...")
    try:
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
 
    # 6. Create Subscription (Using Customer JWT!)
    print("6. Sending subscription creation request to POST /api/v1/subscriptions via Gateway...")
    sub_payload = {
        "customerId": customer_id,
        "tariffId": tariff_id,
        "tariffCode": tariff_code
    }
    print(f"   [REQUEST PAYLOAD] {json.dumps(sub_payload, indent=2)}")
    
    sub_req = urllib.request.Request(
        f"{GATEWAY_BASE}/api/v1/subscriptions",
        data=json.dumps(sub_payload).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {customer_token}"
        },
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

    print("=== KART 14.1 E2E ONBOARDING WITH JWT TEST PASSED ===")

if __name__ == "__main__":
    main()
