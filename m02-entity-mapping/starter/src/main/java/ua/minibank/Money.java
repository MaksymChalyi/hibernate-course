package ua.minibank;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Гроші: сума завжди разом із валютою. «100» без валюти — не гроші: 100 UAH і 100 USD відрізняються в десятки разів.
 * Завдання 4 (резерв): зробити клас вбудованим, як Address.
 */
// TODO 4.1 (резерв) Познач клас @Embeddable, додай @Column(precision = 19, scale = 4) над amount і @Column(length = 3) над currency
public class Money {

    private BigDecimal amount;
    private Currency currency;

    // TODO 4.3 (резерв) Конструктор без аргументів: protected Money() { }

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

    /** compareTo, а не equals: для BigDecimal 100.0 і 100.00 різні за equals, але однакові за значенням. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money other)) {
            return false;
        }
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
