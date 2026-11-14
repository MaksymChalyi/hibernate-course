package ua.minibank;

import java.math.BigDecimal;

/** Залишок після кожної операції. Рахується window function у PostgreSQL. */
public record BalancePoint(Long transactionId, BigDecimal amount, BigDecimal balanceAfter) {
}
