package com.telcox.springmicroservices.billing.dto;

public class UsageOverageSummaryDto {
    private String type; // VOICE, SMS, DATA
    private Double totalOverageAmount;

    public UsageOverageSummaryDto() {}

    public UsageOverageSummaryDto(String type, Double totalOverageAmount) {
        this.type = type;
        this.totalOverageAmount = totalOverageAmount;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getTotalOverageAmount() { return totalOverageAmount; }
    public void setTotalOverageAmount(Double totalOverageAmount) { this.totalOverageAmount = totalOverageAmount; }
}
