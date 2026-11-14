package ua.minibank;

import java.math.BigDecimal;
import java.time.Instant;

/** Рядок історії операцій для показу клієнту. Не entity: Hibernate його не відстежує. */
public record TransactionView(String accountNumber, TransactionType type, BigDecimal amount, Instant createdAt) {
}
