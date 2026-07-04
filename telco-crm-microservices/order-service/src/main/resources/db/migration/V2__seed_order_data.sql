-- Insert a dummy DRAFT order
INSERT INTO orders (id, customer_id, status, total_amount, currency)
VALUES (1001, 5001, 'DRAFT', 250.00, 'TRY');

-- Insert dummy items for the order
INSERT INTO order_item (id, order_id, product_code, product_type, quantity, unit_price)
VALUES (2001, 1001, 'PREMIUM_5G_TARIFF', 'TARIFF', 1, 150.00);

INSERT INTO order_item (id, order_id, product_code, product_type, quantity, unit_price)
VALUES (2002, 1001, 'ROAMING_ADDON_EU', 'ADDON', 2, 50.00);

-- Note: We do NOT insert any rows into saga_state or outbox_event in this seed file
-- as requested by the task constraints.
