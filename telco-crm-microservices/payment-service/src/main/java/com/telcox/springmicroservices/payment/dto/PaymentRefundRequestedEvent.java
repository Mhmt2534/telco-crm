package com.telcox.springmicroservices.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentRefundRequestedEvent {
    private UUID orderId;
    private UUID paymentId;
    private BigDecimal amount;
    private Instant occurredAt;

    public PaymentRefundRequestedEvent() {}

    public PaymentRefundRequestedEvent(UUID orderId, UUID paymentId, BigDecimal amount, Instant occurredAt) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
