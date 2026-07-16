package com.telcox.springmicroservices.subscription.dto;

public class OrderConfirmedEvent {
    private java.util.UUID orderId;
    private java.util.UUID customerId;
    private java.util.UUID productId;
    private String tariffCode;
    private String productType;
    private java.util.UUID subscriptionId;

    public OrderConfirmedEvent() {
    }

    public OrderConfirmedEvent(java.util.UUID orderId, java.util.UUID customerId, java.util.UUID productId, String tariffCode, String productType, java.util.UUID subscriptionId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.tariffCode = tariffCode;
        this.productType = productType;
        this.subscriptionId = subscriptionId;
    }

    public java.util.UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(java.util.UUID orderId) {
        this.orderId = orderId;
    }

    public java.util.UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(java.util.UUID customerId) {
        this.customerId = customerId;
    }

    public java.util.UUID getProductId() { return productId; }
    public void setProductId(java.util.UUID productId) { this.productId = productId; }

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
