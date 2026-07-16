package com.telcox.springmicroservices.payment.domain.entity;

import com.telcox.common.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet extends BaseEntity {

    @Column(name = "customer_public_id", nullable = false, unique = true)
    private UUID customerId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "balance_hash", nullable = false, length = 255)
    private String balanceHash;

    public Wallet() {
    }

    public Wallet(UUID customerId, BigDecimal balance, String balanceHash) {
        this.customerId = customerId;
        this.balance = balance;
        this.balanceHash = balanceHash;
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

    public String getBalanceHash() {
        return balanceHash;
    }

    public void setBalanceHash(String balanceHash) {
        this.balanceHash = balanceHash;
    }
}
