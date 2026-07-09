import json
import time
import subprocess
import requests
import random
from datetime import datetime

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

def run_sql(db_container, database, query):
    try:
        cmd = [
            "docker", "exec", "-i", db_container,
            "psql", "-U", "postgres", "-d", database, "-c", query
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout
    except Exception as e:
        print(f"Error querying db: {e}")
        return None

def curl_container_api(container_name, port, path, method="GET", payload=None):
    try:
        cmd = ["docker", "exec", "-i", container_name, "curl", "-s", "-X", method]
        if payload:
            cmd.extend(["-H", "Content-Type: application/json", "-d", json.dumps(payload)])
        cmd.append(f"http://localhost:{port}{path}")
        
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"Error calling container API: {e.stderr}")
        return None

def main():
    print("--- KART 20: Invoice PDF & MinIO E2E Integration Test ---\n")

    # 1. Register a test customer directly to customer-service container
    print("1. Creating a test customer...")
    phone_num = f"90500{random.randint(1000000, 9999999)}"
    tckn = generate_valid_tckn()
    
    customer_payload = {
        "firstName": "Ahmet",
        "lastName": "Yilmaz",
        "type": "INDIVIDUAL",
        "identityNumber": tckn,
        "dateOfBirth": "1988-10-15",
        "phone": phone_num,
        "email": f"ahmet.yilmaz_{random.randint(1000, 9999)}@example.com",
        "addresses": [
            {
                "line1": "ITU Maslak Kampusu Teknokent",
                "city": "Istanbul",
                "district": "Sariyer",
                "postalCode": "34469",
                "isDefault": True
            }
        ]
    }
    
    resp_str = curl_container_api("customer-service", 9002, "/api/v1/customers", "POST", customer_payload)
    if not resp_str:
        print("FAILED to create customer!")
        return
        
    try:
        customer = json.loads(resp_str)
        customer_id = customer["id"]
    except Exception as e:
        print(f"FAILED to parse customer response: {resp_str}, error: {e}")
        return
        
    print(f"Created Customer ID: {customer_id}")

    # 2. Insert a BillCycle record in billing_db matching today's day of month
    current_day = get_current_day_of_month()
    subscription_id = 1205
    fixed_amount = 180.00
    
    print(f"\n2. Inserting BillCycle for customer {customer_id} on cut_off_day {current_day}...")
    sql_insert = (
        f"INSERT INTO bill_cycle (customer_id, subscription_id, msisdn, cut_off_day, fixed_amount) "
        f"VALUES ({customer_id}, {subscription_id}, '5001234567', {current_day}, {fixed_amount});"
    )
    run_sql("billing-db", "billing_db", sql_insert)
    print("BillCycle inserted into billing_db.")

    # 3. Trigger Bill Run via billing-service container API
    print("\n3. Triggering daily bill run...")
    resp_str = curl_container_api("billing-service", 9007, "/api/v1/billing/runs", "POST")
    print(f"Bill run response: {resp_str}")

    # 4. Fetch the generated invoice ID from DB
    print("\n4. Retrieving generated Invoice ID from DB...")
    time.sleep(2)  # Give it a moment to commit
    sql_select = f"SELECT id FROM invoice WHERE customer_id = {customer_id} ORDER BY id DESC LIMIT 1;"
    db_out = run_sql("billing-db", "billing_db", sql_select)
    print(db_out)
    
    invoice_id = None
    if db_out:
        lines = db_out.strip().split("\n")
        for line in lines:
            val = line.strip()
            if val.isdigit():
                invoice_id = int(val)
                break
                
    if not invoice_id:
        print("FAILED to find generated invoice ID in billing_db!")
        return
    print(f"Detected Invoice ID: {invoice_id}")

    # 5. Wait for Debezium -> Kafka -> InvoiceGeneratedConsumer -> MinIO PDF upload
    print("\n5. Waiting for Debezium/Kafka PDF generation and upload (max 15 seconds)...")
    success = False
    presigned_data = None
    
    for i in range(15):
        time.sleep(1)
        print(f"Checking for PDF link... {i+1}/15")
        # Request the presigned PDF download URL from billing-service container
        pdf_resp_str = curl_container_api("billing-service", 9007, f"/api/v1/invoices/{invoice_id}/pdf", "GET")
        if pdf_resp_str and "pdfUrl" in pdf_resp_str:
            try:
                presigned_data = json.loads(pdf_resp_str)
                success = True
                break
            except Exception:
                pass
            
    if not success or not presigned_data:
        print(f"\nFAILED: PDF URL was not generated! Response: {pdf_resp_str}")
        
        # Check docker logs of billing-service to see what happened
        print("\n--- billing-service logs ---")
        logs = subprocess.run(["docker", "--profile", "apps", "logs", "--tail", "30", "billing-service"], capture_output=True, text=True)
        print(logs.stdout)
        return

    raw_url = presigned_data["pdfUrl"]
    print(f"Presigned URL generated: {raw_url}")

    # 6. Verify URL is reachable and download the PDF
    # Since telco-minio hostname is internal to docker, we replace it with localhost
    download_url = raw_url.replace("telco-minio", "localhost")
    print(f"Downloading PDF from mapped URL: {download_url}")
    
    # We must pass the original Host header (telco-minio:9000) so the S3 V4 Signature matches
    headers = {"Host": "telco-minio:9000"}
    pdf_file_resp = requests.get(download_url, headers=headers)
    if pdf_file_resp.status_code != 200:
        print(f"FAILED to download PDF! Status code: {pdf_file_resp.status_code}, Detail: {pdf_file_resp.text}")
        return
        
    pdf_content = pdf_file_resp.content
    print(f"Downloaded PDF size: {len(pdf_content)} bytes")
    
    if pdf_content.startswith(b"%PDF-"):
        print("\nSUCCESS! The downloaded file is a valid PDF document.")
    else:
        print("\nWARNING! The downloaded file does not start with %PDF- header.")

    print("\n--- Test Completed Successfully ---")

if __name__ == "__main__":
    main()
