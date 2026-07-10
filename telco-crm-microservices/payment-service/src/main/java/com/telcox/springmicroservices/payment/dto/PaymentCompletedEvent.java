package com.telcox.springmicroservices.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentCompletedEvent {
    private UUID paymentId;
    private Long orderId;
    private String customerId;
    private BigDecimal amount;
    private Instant occurredAt;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(UUID paymentId, Long orderId, String customerId, BigDecimal amount, Instant occurredAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
