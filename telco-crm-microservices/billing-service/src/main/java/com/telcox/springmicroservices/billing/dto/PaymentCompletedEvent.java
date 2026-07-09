package com.telcox.springmicroservices.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentCompletedEvent {
    private UUID paymentId;
    private Long orderId;
    private String invoiceId;
    private Long customerId;
    private BigDecimal amount;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(UUID paymentId, Long orderId, String invoiceId, Long customerId, BigDecimal amount) {
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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
