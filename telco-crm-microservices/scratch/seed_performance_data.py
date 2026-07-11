import subprocess
import uuid
import random
from datetime import datetime

def query_db(db_container, database, query):
    try:
        cmd = [
            "docker", "exec", "-i", db_container,
            "psql", "-U", "postgres", "-d", database
        ]
        result = subprocess.run(cmd, input=query, capture_output=True, text=True, check=True)
        return result.stdout
    except Exception as e:
        print(f"Error querying db {database}: {e}")
        return ""

def main():
    print("=== Seeding Performance Data for KART 29 ===")
    
    # 1. Clean up existing billing cycles to ensure clean performance runs
    print("Cleaning up old bill cycle records...")
    query_db("billing-db", "billing_db", "TRUNCATE TABLE bill_cycle CASCADE;")
    query_db("billing-db", "billing_db", "TRUNCATE TABLE invoice CASCADE;")
    
    current_day = datetime.now().day
    print(f"Targeting cut-off day: {current_day}")
    
    # We will generate 1000 insert queries. To do this efficiently, we write them into a SQL script or execute a batch INSERT.
    # Let's generate a single large INSERT statement.
    values = []
    for i in range(1, 1001):
        customer_id = 20000 + i
        sub_id = str(uuid.uuid4())
        msisdn = f"90599{i:07d}"
        fixed_amount = 100.00
        values.append(f"({customer_id}, '{sub_id}', '{msisdn}', {current_day}, {fixed_amount})")
        
    print("Inserting 1000 bill cycle records...")
    batch_insert_query = (
        "INSERT INTO bill_cycle (customer_id, subscription_id, msisdn, cut_off_day, fixed_amount) VALUES "
        + ", ".join(values) + ";"
    )
    
    res = query_db("billing-db", "billing_db", batch_insert_query)
    if "INSERT" in res:
        print("Successfully seeded 1000 billing cycles!")
    else:
        print(f"Seeding failed: {res}")

if __name__ == "__main__":
    main()
