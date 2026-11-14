package ua.minibank;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(Long id, TransactionType type, BigDecimal amount, String description, Instant createdAt) {
}
