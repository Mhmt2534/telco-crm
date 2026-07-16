ALTER TABLE invoice ADD COLUMN customer_public_id UUID;
ALTER TABLE invoice ALTER COLUMN customer_id DROP NOT NULL;

ALTER TABLE bill_cycle ADD COLUMN customer_public_id UUID;
ALTER TABLE bill_cycle ALTER COLUMN customer_id DROP NOT NULL;

CREATE INDEX idx_invoice_customer_public_id ON invoice(customer_public_id);
CREATE INDEX idx_bill_cycle_customer_public_id ON bill_cycle(customer_public_id);
