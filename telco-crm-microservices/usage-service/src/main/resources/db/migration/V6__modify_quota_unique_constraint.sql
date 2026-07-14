ALTER TABLE quotas DROP CONSTRAINT quotas_subscription_id_key;
ALTER TABLE quotas ADD CONSTRAINT quotas_subscription_status_unique UNIQUE (subscription_id, status);
