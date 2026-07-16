package com.telcox.springmicroservices.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentFailedEvent {
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private String errorCode;
    private String errorMessage;
    private Instant occurredAt;

    public PaymentFailedEvent() {
    }

    public PaymentFailedEvent(UUID orderId, UUID customerId, BigDecimal amount, String errorCode, String errorMessage, Instant occurredAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.occurredAt = occurredAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

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

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
