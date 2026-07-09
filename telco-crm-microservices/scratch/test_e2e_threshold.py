import json
import subprocess
import time
import sys
import uuid
import random

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
        print(f"   [ERROR] Failed to query {container_name}: {e}")
        return ""

def publish_kafka_message(topic, message_key, message_val):
    try:
        cmd = [
            "docker", "exec", "-i", "telco-kafka",
            "/opt/bitnami/kafka/bin/kafka-console-producer.sh",
            "--bootstrap-server", "kafka:29092",
            "--topic", topic,
            "--property", "parse.key=true",
            "--property", "key.separator=:"
        ]
        # Format as key:value
        input_data = f"{message_key}:{json.dumps(message_val)}"
        result = subprocess.run(cmd, input=input_data, capture_output=True, text=True, check=True)
        return True
    except Exception as e:
        print(f"   [ERROR] Failed to publish message to Kafka: {e}")
        return False

def main():
    print("=== STARTING KART 20.5 USAGE THRESHOLD E2E TEST ===\n")
    
    sub_id = str(uuid.uuid4())
    msisdn = f"90555{random.randint(1000000, 9999999)}"
    
    print(f"Step 1: Directly inserting a Quota record into usage_db for Subscription: {sub_id}...")
    insert_quota_query = f"""
    INSERT INTO quotas (
        id, subscription_id, period_start, period_end, 
        total_minutes, total_sms, total_mb, 
        minutes_remaining, sms_remaining, mb_remaining, 
        created_at, updated_at, version
    ) VALUES (
        gen_random_uuid(), '{sub_id}', now(), now() + interval '1 month',
        100, 100, 100,
        100, 100, 100,
        now(), now(), 0
    );
    """
    query_db("usage-db", "usage_db", insert_quota_query)
    
    # Verify insert
    verify_quota = query_db("usage-db", "usage_db", f"SELECT subscription_id, minutes_remaining FROM quotas WHERE subscription_id = '{sub_id}';")
    if sub_id not in verify_quota:
        print("   [FAILED] Quota record was not inserted successfully.")
        sys.exit(1)
    print("   [SUCCESS] Quota record inserted.")

    # Step 2: Send 75 units of VOICE consumption (75% usage, no threshold yet)
    print("\nStep 2: Publishing VOICE consumption event of 75 units (Usage: 75% / Threshold: 80%)...")
    cdr_event_1 = {
        "subscriptionId": sub_id,
        "msisdn": msisdn,
        "type": "VOICE",
        "amount": 75.0,
        "cdrRef": f"CDR-TEST-{random.randint(10000, 99999)}",
        "recordedAt": "2026-07-09T08:00:00Z"
    }
    publish_kafka_message("telco.usage.events", msisdn, cdr_event_1)
    
    print("Waiting 3 seconds for usage-service to process...")
    time.sleep(3)
    
    print("Verifying remaining voice quota and outbox...")
    db_state_1 = query_db("usage-db", "usage_db", f"SELECT minutes_remaining, voice_threshold_reached FROM quotas WHERE subscription_id = '{sub_id}';")
    print(db_state_1.strip())
    
    events_1 = query_db("usage-db", "usage_db", f"SELECT event_type, payload FROM outbox_event WHERE aggregate_id = '{sub_id}';")
    print("=== outbox_event Table ===")
    print(events_1.strip())
    
    if "QuotaThresholdReached" in events_1:
        print("   [FAILED] QuotaThresholdReached event generated prematurely at 75% usage.")
        sys.exit(1)
    print("   [PASS] No threshold event generated at 75% usage.")

    # Step 3: Send 10 units of VOICE consumption (85% usage, triggers 80% threshold)
    print("\nStep 3: Publishing VOICE consumption event of 10 units (Total Usage: 85% / Threshold: 80%)...")
    cdr_event_2 = {
        "subscriptionId": sub_id,
        "msisdn": msisdn,
        "type": "VOICE",
        "amount": 10.0,
        "cdrRef": f"CDR-TEST-{random.randint(10000, 99999)}",
        "recordedAt": "2026-07-09T08:05:00Z"
    }
    publish_kafka_message("telco.usage.events", msisdn, cdr_event_2)
    
    print("Waiting 3 seconds for usage-service to process...")
    time.sleep(3)
    
    print("Verifying remaining voice quota and outbox...")
    db_state_2 = query_db("usage-db", "usage_db", f"SELECT minutes_remaining, voice_threshold_reached FROM quotas WHERE subscription_id = '{sub_id}';")
    print(db_state_2.strip())
    
    events_2 = query_db("usage-db", "usage_db", f"SELECT event_type, payload FROM outbox_event WHERE aggregate_id = '{sub_id}';")
    print("=== outbox_event Table ===")
    print(events_2.strip())
    
    if "QuotaThresholdReached" not in events_2:
        print("   [FAILED] QuotaThresholdReached event was not generated at 85% usage.")
        sys.exit(1)
    print("   [PASS] QuotaThresholdReached event successfully generated at 85% usage!")

    # Step 4: Send 20 units of VOICE consumption (Total Usage: 105% (capped at 100%), triggers QuotaExceeded)
    print("\nStep 4: Publishing VOICE consumption event of 20 units (Total Usage: 100% / QuotaExceeded)...")
    cdr_event_3 = {
        "subscriptionId": sub_id,
        "msisdn": msisdn,
        "type": "VOICE",
        "amount": 20.0,
        "cdrRef": f"CDR-TEST-{random.randint(10000, 99999)}",
        "recordedAt": "2026-07-09T08:10:00Z"
    }
    publish_kafka_message("telco.usage.events", msisdn, cdr_event_3)
    
    print("Waiting 3 seconds for usage-service to process...")
    time.sleep(3)
    
    print("Verifying remaining voice quota and outbox...")
    db_state_3 = query_db("usage-db", "usage_db", f"SELECT minutes_remaining, voice_exceeded FROM quotas WHERE subscription_id = '{sub_id}';")
    print(db_state_3.strip())
    
    events_3 = query_db("usage-db", "usage_db", f"SELECT event_type, payload FROM outbox_event WHERE aggregate_id = '{sub_id}';")
    print("=== outbox_event Table ===")
    print(events_3.strip())
    
    if "QuotaExceeded" not in events_3:
        print("   [FAILED] QuotaExceeded event was not generated at 100% usage.")
        sys.exit(1)
    print("   [PASS] QuotaExceeded event successfully generated at 100% usage!")

    print("\n=== ALL KART 20.5 TEST CASES PASSED SUCCESSFULLY ===")

if __name__ == "__main__":
    main()
