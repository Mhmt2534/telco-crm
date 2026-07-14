package com.telcox.springmicroservices.subscription.dto;

public class OrderConfirmedEvent {
    private Long orderId;
    private Long customerId;
    private String tariffCode;
    private String productType;
    private java.util.UUID subscriptionId;

    public OrderConfirmedEvent() {
    }

    public OrderConfirmedEvent(Long orderId, Long customerId, String tariffCode, String productType, java.util.UUID subscriptionId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.tariffCode = tariffCode;
        this.productType = productType;
        this.subscriptionId = subscriptionId;
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

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public java.util.UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(java.util.UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public String toString() {
        return "OrderConfirmedEvent{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", tariffCode='" + tariffCode + '\'' +
                ", productType='" + productType + '\'' +
                ", subscriptionId=" + subscriptionId +
                '}';
    }
}
