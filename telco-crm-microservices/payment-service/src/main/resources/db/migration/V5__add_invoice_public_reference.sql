ALTER TABLE payments ADD COLUMN invoice_public_id UUID;
CREATE INDEX idx_payments_invoice_public_id ON payments(invoice_public_id);
