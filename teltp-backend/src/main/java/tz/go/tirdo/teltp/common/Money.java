package tz.go.tirdo.teltp.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Currency;

/** Value object for monetary amounts. Defaults to TZS. */
@Embeddable
public record Money(
        @Column(name = "amount", precision = 14, scale = 2) BigDecimal amount,
        @Column(name = "currency", length = 3) String currency
) {
    public static Money tzs(BigDecimal amount) {
        return new Money(amount, "TZS");
    }

    public static Money zeroTzs() {
        return new Money(BigDecimal.ZERO, "TZS");
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money times(int qty) {
        return new Money(amount.multiply(BigDecimal.valueOf(qty)), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
        Currency.getInstance(currency);
    }
}
