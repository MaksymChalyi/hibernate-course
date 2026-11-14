package ua.minibank;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Фільтр пошуку операцій. Будь-яке поле може бути null — тоді умова не застосовується.
 * Саме через довільний набір умов тут зручний Criteria API, а не рядок JPQL.
 */
public record TransactionFilter(String accountNumber, CurrencyCode currency, TransactionType type,
                                BigDecimal minAmount, BigDecimal maxAmount, Instant from, Instant to) {

    public static TransactionFilter empty() {
        return new TransactionFilter(null, null, null, null, null, null, null);
    }

    public TransactionFilter withAccount(String number) {
        return new TransactionFilter(number, currency, type, minAmount, maxAmount, from, to);
    }

    public TransactionFilter withCurrency(CurrencyCode value) {
        return new TransactionFilter(accountNumber, value, type, minAmount, maxAmount, from, to);
    }

    public TransactionFilter withType(TransactionType value) {
        return new TransactionFilter(accountNumber, currency, value, minAmount, maxAmount, from, to);
    }

    public TransactionFilter withMinAmount(String value) {
        return new TransactionFilter(accountNumber, currency, type, new BigDecimal(value), maxAmount, from, to);
    }

    public TransactionFilter withMaxAmount(String value) {
        return new TransactionFilter(accountNumber, currency, type, minAmount, new BigDecimal(value), from, to);
    }

    public TransactionFilter withPeriod(Instant fromInclusive, Instant toExclusive) {
        return new TransactionFilter(accountNumber, currency, type, minAmount, maxAmount, fromInclusive, toExclusive);
    }
}
