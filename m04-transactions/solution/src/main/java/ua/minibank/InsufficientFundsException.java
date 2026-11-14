package ua.minibank;

import java.math.BigDecimal;

public class InsufficientFundsException extends BankException {

    public InsufficientFundsException(String accountNumber, BigDecimal balance, BigDecimal amount) {
        super("Недостатньо коштів на рахунку " + accountNumber
                + ": баланс " + balance.toPlainString() + ", потрібно " + amount.toPlainString());
    }
}
