package com.telcox.springmicroservices.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionActivatedEvent {
    private Long id; // Bu asıl subscriptionId
    private Long customerId;
    private String msisdn;
    private String tariffCode;
    private String status;
    private Instant activatedAt;

    public SubscriptionActivatedEvent() {}

    public SubscriptionActivatedEvent(Long id, Long customerId, String msisdn, String tariffCode, String status, Instant activatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.msisdn = msisdn;
        this.tariffCode = tariffCode;
        this.status = status;
        this.activatedAt = activatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
