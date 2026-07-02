package com.telcox.springmicroservices.usage.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CdrRecordedEvent {

    private UUID subscriptionId;
    private String msisdn;
    private String type; // VOICE, SMS, DATA
    private Double amount;
    private String cdrRef;
    private OffsetDateTime recordedAt;

    public CdrRecordedEvent() {
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCdrRef() {
        return cdrRef;
    }

    public void setCdrRef(String cdrRef) {
        this.cdrRef = cdrRef;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
