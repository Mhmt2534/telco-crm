package com.telcox.springmicroservices.payment.dto;

import java.math.BigDecimal;

public class OrderCreatedEvent {
    private Long orderId;
    private Long customerId;
    private BigDecimal totalAmount;
    private String occurredAt;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(Long orderId, Long customerId, BigDecimal totalAmount, String occurredAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.occurredAt = occurredAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }

    @Override
    public String toString() {
        return "OrderCreatedEvent{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", totalAmount=" + totalAmount +
                ", occurredAt='" + occurredAt + '\'' +
                '}';
    }
}
