package ua.minibank;

public class AccountNotFoundException extends BankException {

    public AccountNotFoundException(Long accountId) {
        super("Рахунок " + accountId + " не знайдено");
    }
}
