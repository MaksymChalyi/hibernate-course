package ua.minibank;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Рахунок. Зв'язок із клієнтом додамо в модулі 5.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq")
    @SequenceGenerator(name = "account_seq", sequenceName = "account_seq", allocationSize = 50)
    private Long id;

    /** IBAN: до 34 символів. */
    @Column(nullable = false, unique = true, length = 34)
    private String number;

    @Convert(converter = CurrencyConverter.class)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /**
     * Гроші — тільки BigDecimal. precision = 19 — усього цифр, scale = 4 — після коми.
     * Чотири знаки після коми потрібні для проміжних розрахунків (відсотки, курси).
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /** Резерв: один embeddable двічі в одній таблиці потребує перейменування колонок. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "credit_limit_amount", precision = 19, scale = 4)),
            @AttributeOverride(name = "currency", column = @Column(name = "credit_limit_currency", length = 3))
    })
    private Money creditLimit;

    protected Account() {
    }

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
