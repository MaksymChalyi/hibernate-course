package ua.minibank;

public class CurrencyMismatchException extends BankException {

    public CurrencyMismatchException(CurrencyCode from, CurrencyCode to) {
        super("Переказ між рахунками в різних валютах не підтримується: " + from + " → " + to);
    }
}
