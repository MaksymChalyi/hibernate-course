package ua.minibank.domain;

/**
 * Базовий клас бізнес-помилок. RuntimeException, бо помилку не можна виправити
 * на місці виклику: її треба донести до користувача.
 */
public abstract class BankException extends RuntimeException {

    protected BankException(String message) {
        super(message);
    }
}
