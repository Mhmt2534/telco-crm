package com.telcox.common.persistence.converter;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Transparently encrypts PII string columns (e.g. TCKN/VKN, card number) at rest using
 * AES-256-GCM. Apply explicitly with {@code @Convert(converter = PiiEncryptionConverter.class)}
 * on the field — it is deliberately NOT {@code autoApply} so non-PII strings are untouched.
 *
 * <p>Storage format: Base64( IV(12 bytes) || ciphertext+tag ).</p>
 *
 * <p>The 256-bit key is read (Base64) from the {@code TELCO_PII_ENCRYPTION_KEY} environment
 * variable or the {@code telco.pii.encryption-key} system property. In production this value
 * comes from Vault / Kubernetes Secrets — never hardcode or commit it.</p>
 */
@Converter
public class PiiEncryptionConverter implements AttributeConverter<String, String> {

    private static final String ENV_KEY = "TELCO_PII_ENCRYPTION_KEY";
    private static final String PROP_KEY = "telco.pii.encryption-key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private volatile SecretKeySpec cachedKey;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt PII value", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt PII value", e);
        }
    }

    private SecretKeySpec key() {
        SecretKeySpec local = cachedKey;
        if (local != null) {
            return local;
        }
        String base64Key = System.getenv(ENV_KEY);
        if (base64Key == null || base64Key.isBlank()) {
            base64Key = System.getProperty(PROP_KEY);
        }
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "PII encryption key not configured. Set env %s or system property %s (Base64-encoded 256-bit key)."
                            .formatted(ENV_KEY, PROP_KEY));
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());
        if (keyBytes.length != 32) {
            throw new IllegalStateException("PII encryption key must be a Base64-encoded 256-bit (32-byte) key.");
        }
        local = new SecretKeySpec(keyBytes, "AES");
        cachedKey = local;
        return local;
    }
}
