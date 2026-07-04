package com.telcox.springmicroservices.cdrsimulator.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * CDR (Call Detail Record) event'i.
 * usage-service'teki CdrRecordedEvent ile birebir aynı yapıdadır.
 * Kafka'daki telco.usage.events topic'ine bu nesne JSON olarak basılır.
 */
public class CdrRecordedEvent {

    private UUID subscriptionId;
    private String msisdn;
    private String type; // VOICE, SMS, DATA
    private Double amount;
    private String cdrRef;
    private Instant recordedAt;

    public CdrRecordedEvent() {
    }

    public CdrRecordedEvent(UUID subscriptionId, String msisdn, String type,
                             Double amount, String cdrRef, Instant recordedAt) {
        this.subscriptionId = subscriptionId;
        this.msisdn = msisdn;
        this.type = type;
        this.amount = amount;
        this.cdrRef = cdrRef;
        this.recordedAt = recordedAt;
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

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
