package com.telcox.springmicroservices.payment.service;

import com.telcox.springmicroservices.payment.domain.entity.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class WalletSecurityService {

    private static final Logger log = LoggerFactory.getLogger(WalletSecurityService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String secretKey;

    public WalletSecurityService(@Value("${app.wallet.secret-key:telco-crm-secure-wallet-secret-key-32-chars-long}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String calculateHash(UUID customerId, BigDecimal balance) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId cannot be null");
        }
        BigDecimal normalizedBalance = balance == null ? BigDecimal.ZERO : balance.setScale(2, RoundingMode.HALF_UP);
        String data = customerId + ":" + normalizedBalance.toString();

        try {
            Mac sha256HMAC = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            sha256HMAC.init(secretKeySpec);
            byte[] hashBytes = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to calculate HMAC-SHA256 hash for customer {}", customerId, e);
            throw new RuntimeException("HMAC calculation failure", e);
        }
    }

    public void verifyHash(Wallet wallet) {
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet cannot be null");
        }
        String calculated = calculateHash(wallet.getCustomerId(), wallet.getBalance());
        if (!calculated.equals(wallet.getBalanceHash())) {
            log.error("Wallet tampering detected for customer ID: {}. Expected hash: {}, Found: {}",
                    wallet.getCustomerId(), calculated, wallet.getBalanceHash());
            throw new SecurityException("Wallet integrity check failed! Tampering detected.");
        }
    }
}
