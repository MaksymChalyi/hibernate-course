package ua.minibank.service;

import ua.minibank.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(String accountNumber, TransactionType type, BigDecimal amount, Instant createdAt) {
}
