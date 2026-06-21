package com.telcox.common.core.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable money value object. Monetary amounts are always {@link BigDecimal} with an explicit
 * ISO-4217 currency code (default {@code TRY}); never use double/float for money.
 */
public record MoneyValueObject(BigDecimal amount, String currency) {

    public static final String DEFAULT_CURRENCY = "TRY";

    public MoneyValueObject {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
    }

    public static MoneyValueObject of(BigDecimal amount) {
        return new MoneyValueObject(amount, DEFAULT_CURRENCY);
    }

    public static MoneyValueObject of(BigDecimal amount, String currency) {
        return new MoneyValueObject(amount, currency);
    }

    public MoneyValueObject add(MoneyValueObject other) {
        requireSameCurrency(other);
        return new MoneyValueObject(this.amount.add(other.amount), this.currency);
    }

    public MoneyValueObject subtract(MoneyValueObject other) {
        requireSameCurrency(other);
        return new MoneyValueObject(this.amount.subtract(other.amount), this.currency);
    }

    private void requireSameCurrency(MoneyValueObject other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: %s vs %s".formatted(this.currency, other.currency));
        }
    }
}
