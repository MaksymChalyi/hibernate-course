package ua.minibank.api;

import java.math.BigDecimal;

public record TransferResult(String status, Long fromAccountId, Long toAccountId, BigDecimal amount) {

    public static TransferResult completed(TransferRequest request) {
        return new TransferResult("COMPLETED", request.fromAccountId(), request.toAccountId(), request.amount());
    }
}
