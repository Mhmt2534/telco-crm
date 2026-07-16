package com.telcox.springmicroservices.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletBalanceResponse {
    private UUID customerId;
    private BigDecimal balance;

    public WalletBalanceResponse() {
    }

    public WalletBalanceResponse(UUID customerId, BigDecimal balance) {
        this.customerId = customerId;
        this.balance = balance;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
