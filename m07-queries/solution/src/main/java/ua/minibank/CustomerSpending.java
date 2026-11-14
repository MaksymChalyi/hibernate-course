package ua.minibank;

import java.math.BigDecimal;

public record CustomerSpending(Long customerId, String firstName, String lastName, BigDecimal totalSpent) {
}
