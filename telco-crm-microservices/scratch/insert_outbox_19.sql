INSERT INTO outbox_event (aggregate_type, aggregate_id, event_type, payload)
VALUES ('Order', '19', 'OrderConfirmed', '{"orderId": 19, "customerId": 21, "tariffCode": "TRF-HAPPY"}'::jsonb);
