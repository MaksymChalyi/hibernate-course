package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Резерв: сума завжди разом із валютою.
 * 100 без валюти — не гроші: 100 UAH і 100 USD відрізняються в ~40 разів.
 */
@Embeddable
public class Money {

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Convert(converter = CurrencyConverter.class)
    @Column(length = 3)
    private Currency currency;

    protected Money() {
    }

    public Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount);
        this.currency = Objects.requireNonNull(currency);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money other)) {
            return false;
        }
        // compareTo, а не equals: для BigDecimal 100.0 і 100.00 — різні за equals
        return amount.compareTo(other.amount) == 0 && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
