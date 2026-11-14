package ua.minibank.api;

import java.math.BigDecimal;

/** Тіло POST /transfers: {"fromAccountId": 1, "toAccountId": 3, "amount": 250.00} */
public record TransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount) {
}
