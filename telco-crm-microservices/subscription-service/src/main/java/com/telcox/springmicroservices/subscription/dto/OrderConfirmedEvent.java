package com.telcox.springmicroservices.subscription.dto;

public class OrderConfirmedEvent {
    private Long orderId;
    private Long customerId;
    private String tariffCode;

    public OrderConfirmedEvent() {
    }

    public OrderConfirmedEvent(Long orderId, Long customerId, String tariffCode) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.tariffCode = tariffCode;
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

    public String getTariffCode() {
        return tariffCode;
    }

    public void setTariffCode(String tariffCode) {
        this.tariffCode = tariffCode;
    }

    @Override
    public String toString() {
        return "OrderConfirmedEvent{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", tariffCode='" + tariffCode + '\'' +
                '}';
    }
}
