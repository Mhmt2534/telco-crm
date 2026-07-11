package com.telcox.springmicroservices.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionActivatedEvent {
    private UUID subscriptionId; // Bu asıl subscriptionId
    private Long customerId;
    private String msisdn;
    private String tariffCode;
    private String status;
    private Instant activatedAt;

    public SubscriptionActivatedEvent() {}

    public SubscriptionActivatedEvent(UUID subscriptionId, Long customerId, String msisdn, String tariffCode, String status, Instant activatedAt) {
        this.subscriptionId = subscriptionId;
        this.customerId = customerId;
        this.msisdn = msisdn;
        this.tariffCode = tariffCode;
        this.status = status;
        this.activatedAt = activatedAt;
    }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public String getTariffCode() { return tariffCode; }
    public void setTariffCode(String tariffCode) { this.tariffCode = tariffCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
}
