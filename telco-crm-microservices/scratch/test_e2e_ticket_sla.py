import json
import subprocess
import time
import sys
import uuid

def query_db(container_name, db_name, query_str):
    try:
        cmd = [
            "docker", "exec", "-i", container_name,
            "psql", "-U", "postgres", "-d", db_name, "-c", query_str
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"Error querying db: {e.stderr}")
        return None

def curl_api(container_name, url, method="GET", payload=None):
    try:
        cmd = ["docker", "exec", "-i", container_name, "curl", "-s", "-X", method]
        if payload:
            cmd.extend(["-H", "Content-Type: application/json", "-d", json.dumps(payload)])
        cmd.append(url)
        
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"Error calling API: {e.stderr}")
        return None

def consume_kafka_messages(topic, timeout_sec=10):
    try:
        cmd = [
            "docker", "exec", "-i", "telco-kafka",
            "/opt/bitnami/kafka/bin/kafka-console-consumer.sh",
            "--bootstrap-server", "kafka:29092",
            "--topic", topic,
            "--from-beginning",
            "--timeout-ms", str(timeout_sec * 1000)
        ]
        result = subprocess.run(cmd, capture_output=True, text=True)
        return result.stdout
    except Exception as e:
        print(f"Error consuming kafka: {e}")
        return None

def main():
    print("--- KART 21: Ticket E2E SLA Test ---")
    
    customer_id = str(uuid.uuid4())
    payload = {
        "customerId": customer_id,
        "category": "TECHNICAL_ISSUE",
        "description": "Internet connection drops frequently",
        "priority": "CRITICAL"
    }
    
    print("\n1. Creating ticket via API...")
    resp_str = curl_api("customer-service", "http://ticket-service:9010/api/v1/tickets", "POST", payload)
    print(f"Response: {resp_str}")
    
    if not resp_str:
        print("Failed to create ticket")
        sys.exit(1)
        
    try:
        resp_json = json.loads(resp_str)
        ticket_id = resp_json.get("id")
    except Exception as e:
        print(f"Failed to parse json: {e}")
        sys.exit(1)
        
    print(f"Created Ticket ID: {ticket_id}")
    
    time.sleep(2)
    
    print("\n2. Checking Outbox table for TicketOpened event...")
    outbox_query = f"SELECT event_type, payload FROM outbox_event WHERE aggregate_id = '{ticket_id}' AND event_type = 'TicketOpened';"
    outbox_res = query_db("ticket-db", "ticket_db", outbox_query)
    print(f"Outbox Event:\n{outbox_res}")
    
    print("\n3. Simulating SLA Breach by modifying sla_due_at in DB...")
    update_query = f"UPDATE tickets SET sla_due_at = CURRENT_TIMESTAMP - INTERVAL '1 hour' WHERE id = '{ticket_id}';"
    query_db("ticket-db", "ticket_db", update_query)
    
    print("\n4. Waiting for Scheduler to trigger (SLA Check runs every minute)...")
    for i in range(1, 65):
        sys.stdout.write(f"\rWaiting {i}/65 seconds...")
        sys.stdout.flush()
        time.sleep(1)
        
    print("\n\n5. Checking DB for BREACHED status and SlaBreachedEvent...")
    ticket_query = f"SELECT status FROM tickets WHERE id = '{ticket_id}';"
    ticket_res = query_db("ticket-db", "ticket_db", ticket_query)
    print(f"Ticket Status:\n{ticket_res}")
    if "BREACHED" not in str(ticket_res):
        print("WARNING: Ticket is not BREACHED!")
    
    outbox_query = f"SELECT event_type, payload FROM outbox_event WHERE aggregate_id = '{ticket_id}' AND event_type = 'SlaBreachedEvent';"
    outbox_res = query_db("ticket-db", "ticket_db", outbox_query)
    print(f"Outbox SlaBreachedEvent:\n{outbox_res}")
    
    if "SlaBreachedEvent" not in str(outbox_res):
        print("WARNING: SlaBreachedEvent not found in outbox!")

    print("\n6. Checking Kafka for events...")
    kafka_output = consume_kafka_messages("telcox.Ticket.events", timeout_sec=10)
    print("Kafka Messages:")
    print(kafka_output)
    
    if ticket_id in str(kafka_output):
        print("SUCCESS! Events propagated to Kafka.")
    else:
        print("FAILED! Events not found in Kafka.")
        
    print("\n--- Test Completed ---")

if __name__ == "__main__":
    main()
