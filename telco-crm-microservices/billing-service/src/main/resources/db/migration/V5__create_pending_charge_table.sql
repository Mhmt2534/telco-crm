CREATE TABLE pending_charge (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    subscription_id UUID NOT NULL,
    customer_public_id UUID NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    effective_bill_cycle VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pending_charge_sub_status ON pending_charge(subscription_id, status);
