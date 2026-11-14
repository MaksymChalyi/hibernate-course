package ua.minibank;

import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * TODO 3. Зроби клас entity для таблиці accounts:
 *   - id: SEQUENCE-генератор "account_seq", allocationSize = 50;
 *   - number: not null, unique, довжина 34 (IBAN);
 *   - currency: not null, довжина 3. Hibernate 6 мапить java.util.Currency сам, як "UAH";
 *   - balance: not null, precision = 19, scale = 4.
 */
public class Account {

    private Long id;
    private String number;
    private Currency currency;
    private BigDecimal balance;

    /**
     * TODO 4 (резерв): прибери @Transient і замапь як @Embedded з колонками
     * credit_limit_amount і credit_limit_currency (@AttributeOverrides).
     * Спочатку зроби Money вбудованим класом.
     */
    @Transient
    private Money creditLimit;

    public Account(String number, Currency currency, BigDecimal balance) {
        this.number = number;
        this.currency = currency;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Money getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(Money creditLimit) {
        this.creditLimit = creditLimit;
    }

    @Override
    public String toString() {
        return "Account{id=" + id + ", number='" + number + "', balance=" + balance.toPlainString() + " " + currency + "}";
    }
}
