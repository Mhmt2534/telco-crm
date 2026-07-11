package com.telcox.springmicroservices.payment.dto;

import java.math.BigDecimal;

public class WalletBalanceResponse {
    private String customerId;
    private BigDecimal balance;

    public WalletBalanceResponse() {
    }

    public WalletBalanceResponse(String customerId, BigDecimal balance) {
        this.customerId = customerId;
        this.balance = balance;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
