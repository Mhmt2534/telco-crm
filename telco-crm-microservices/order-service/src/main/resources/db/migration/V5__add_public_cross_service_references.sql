ALTER TABLE orders ADD COLUMN customer_public_id UUID;
ALTER TABLE orders ALTER COLUMN customer_id DROP NOT NULL;

ALTER TABLE order_item ADD COLUMN product_public_id UUID;

CREATE INDEX idx_orders_customer_public_id ON orders(customer_public_id);
CREATE INDEX idx_order_item_product_public_id ON order_item(product_public_id);
