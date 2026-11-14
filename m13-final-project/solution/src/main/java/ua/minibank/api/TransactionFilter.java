package ua.minibank.api;

import ua.minibank.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Фільтр історії операцій. null у полі — умова не застосовується.
 * from — включно, to — виключно.
 */
public record TransactionFilter(TransactionType type, Instant from, Instant to,
                                BigDecimal minAmount, BigDecimal maxAmount) {

    public static TransactionFilter none() {
        return new TransactionFilter(null, null, null, null, null);
    }
}
