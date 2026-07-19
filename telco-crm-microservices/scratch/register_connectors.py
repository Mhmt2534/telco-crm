import os
import json
import requests

import time

CONNECTORS_DIR = r"c:\Users\msi-nb\Desktop\telco-crm\telco-crm-microservices\debezium\connectors"
CONNECT_URL = "http://localhost:8083/connectors"

def register_connectors():
    print("=== Registering Debezium Connectors ===")
    
    # Wait for Kafka Connect to start up
    print("Waiting for Kafka Connect to become available at http://localhost:8083...")
    connected = False
    for i in range(15):
        try:
            r = requests.get(CONNECT_URL, timeout=5)
            if r.status_code == 200 or r.status_code == 201 or r.status_code == 404:
                connected = True
                print("Kafka Connect is ready!")
                break
        except Exception:
            pass
        print(f"  Attempt {i+1}/15: Kafka Connect not ready yet. Sleeping 10s...")
        time.sleep(10)

    if not connected:
        print("[FAIL] Kafka Connect did not become ready in time.")
        return

    if not os.path.exists(CONNECTORS_DIR):
        print(f"Directory not found: {CONNECTORS_DIR}")
        return

    files = [f for f in os.listdir(CONNECTORS_DIR) if f.endswith(".json")]
    for filename in files:
        filepath = os.path.join(CONNECTORS_DIR, filename)
        with open(filepath, "r", encoding="utf-8") as f:
            try:
                config_data = json.load(f)
            except Exception as e:
                print(f"Failed to parse {filename}: {e}")
                continue

        connector_name = config_data.get("name")
        print(f"Registering {connector_name}...")
        
        # Check if already exists
        check_url = f"{CONNECT_URL}/{connector_name}"
        r = requests.get(check_url)
        if r.status_code == 200:
            print(f"  Connector {connector_name} already exists. Deleting first to update...")
            requests.delete(check_url)

        # Register
        headers = {"Content-Type": "application/json"}
        r = requests.post(CONNECT_URL, json=config_data, headers=headers)
        if r.status_code in [200, 201]:
            print(f"  Successfully registered {connector_name}!")
        else:
            print(f"  Failed to register {connector_name}: {r.status_code} - {r.text}")

if __name__ == "__main__":
    register_connectors()
