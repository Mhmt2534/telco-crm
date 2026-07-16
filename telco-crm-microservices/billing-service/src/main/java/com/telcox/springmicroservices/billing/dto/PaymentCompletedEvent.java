package com.telcox.springmicroservices.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentCompletedEvent {
    private UUID paymentId;
    private UUID orderId;
    private UUID invoiceId;
    private UUID customerId;
    private BigDecimal amount;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(UUID paymentId, UUID orderId, UUID invoiceId, UUID customerId, BigDecimal amount) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.amount = amount;
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

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
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
}
