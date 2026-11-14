package ua.minibank.api;

import ua.minibank.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDto(Long id, TransactionType type, BigDecimal amount, String description, Instant createdAt) {
}
