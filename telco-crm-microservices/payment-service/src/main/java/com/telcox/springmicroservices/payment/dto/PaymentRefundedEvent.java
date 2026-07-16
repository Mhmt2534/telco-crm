package com.telcox.springmicroservices.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentRefundedEvent {
    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private Instant occurredAt;

    public PaymentRefundedEvent() {}

    public PaymentRefundedEvent(UUID paymentId, UUID orderId, BigDecimal amount, Instant occurredAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
