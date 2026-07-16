package com.telcox.springmicroservices.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentCompletedEvent {
    private UUID paymentId;
    private UUID orderId;
    private UUID invoiceId;
    private UUID customerId;
    private BigDecimal amount;
    private Instant occurredAt;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(UUID paymentId, UUID orderId, UUID invoiceId, UUID customerId, BigDecimal amount, Instant occurredAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.invoiceId = invoiceId;
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

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
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
