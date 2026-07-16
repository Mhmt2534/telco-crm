package com.telcox.springmicroservices.subscription.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TariffChangeRequestedEvent {

    private UUID orderId;
    private UUID subscriptionId;
    private UUID customerId;
    private UUID oldTariffId;
    private UUID newTariffId;
    private String oldTariffCode;
    private String newTariffCode;
    private BigDecimal priceDiff;
    private String effectiveBillCycle;
    private Instant occurredAt;

    public TariffChangeRequestedEvent() {
    }

    public TariffChangeRequestedEvent(UUID orderId, UUID subscriptionId, UUID customerId,
                                      String oldTariffCode, String newTariffCode, BigDecimal priceDiff,
                                      String effectiveBillCycle, Instant occurredAt) {
        this.orderId = orderId;
        this.subscriptionId = subscriptionId;
        this.customerId = customerId;
        this.oldTariffCode = oldTariffCode;
        this.newTariffCode = newTariffCode;
        this.priceDiff = priceDiff;
        this.effectiveBillCycle = effectiveBillCycle;
        this.occurredAt = occurredAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getOldTariffId() { return oldTariffId; }
    public void setOldTariffId(UUID oldTariffId) { this.oldTariffId = oldTariffId; }
    public UUID getNewTariffId() { return newTariffId; }
    public void setNewTariffId(UUID newTariffId) { this.newTariffId = newTariffId; }

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
