package com.telcox.springmicroservices.usage.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TariffChangeRequestedEvent {

    private Long orderId;
    private UUID subscriptionId;
    private Long customerId;
    private String oldTariffCode;
    private String newTariffCode;
    private BigDecimal priceDiff;
    private String effectiveBillCycle;
    private Instant occurredAt;

    public TariffChangeRequestedEvent() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getOldTariffCode() {
        return oldTariffCode;
    }

    public void setOldTariffCode(String oldTariffCode) {
        this.oldTariffCode = oldTariffCode;
    }

    public String getNewTariffCode() {
        return newTariffCode;
    }

    public void setNewTariffCode(String newTariffCode) {
        this.newTariffCode = newTariffCode;
    }

    public BigDecimal getPriceDiff() {
        return priceDiff;
    }

    public void setPriceDiff(BigDecimal priceDiff) {
        this.priceDiff = priceDiff;
    }

    public String getEffectiveBillCycle() {
        return effectiveBillCycle;
    }

    public void setEffectiveBillCycle(String effectiveBillCycle) {
        this.effectiveBillCycle = effectiveBillCycle;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
