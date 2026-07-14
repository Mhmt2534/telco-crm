package com.telcox.springmicroservices.usage.dto;

import java.math.BigDecimal;

public class TariffDto {
    private String code;
    private Integer minutesIncluded;
    private Integer smsIncluded;
    private Long dataMbIncluded;
    private BigDecimal monthlyFee;

    public TariffDto() {}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getMinutesIncluded() {
        return minutesIncluded;
    }

    public void setMinutesIncluded(Integer minutesIncluded) {
        this.minutesIncluded = minutesIncluded;
    }

    public Integer getSmsIncluded() {
        return smsIncluded;
    }

    public void setSmsIncluded(Integer smsIncluded) {
        this.smsIncluded = smsIncluded;
    }

    public Long getDataMbIncluded() {
        return dataMbIncluded;
    }

    public void setDataMbIncluded(Long dataMbIncluded) {
        this.dataMbIncluded = dataMbIncluded;
    }

    public BigDecimal getMonthlyFee() {
        return monthlyFee;
    }

    public void setMonthlyFee(BigDecimal monthlyFee) {
        this.monthlyFee = monthlyFee;
    }
}
