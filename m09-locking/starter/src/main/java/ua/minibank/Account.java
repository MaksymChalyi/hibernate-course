package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq")
    @SequenceGenerator(name = "account_seq", sequenceName = "account_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, length = 34)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /*
     * Завдання 1. Optimistic locking: поле версії.
     *
     * Додай поле:
     *     @Version
     *     private Long version;
     * (import jakarta.persistence.Version) і getter getVersion(). Сеттер НЕ потрібен — версію змінює лише Hibernate.
     *
     * Що зміниться: кожен UPDATE стане таким:
     *     update accounts set balance = ?, version = 6 where id = ? and version = 5
     * Якщо хтось уже змінив рядок, версія в базі інша → UPDATE змінить 0 рядків →
     * Hibernate кине OptimisticLockException замість того, щоб мовчки перезаписати чужі зміни.
     *
     * Перевірка: тест «1 · конфлікт версій виявляється». Подивись у консолі на "and version=?".
     */
    // TODO 1 Додати поле @Version private Long version і getter getVersion()

    protected Account() {
    }

    public Account(String number, CurrencyCode currency, BigDecimal balance) {
        this.number = number;
        this.currency = currency;
        this.balance = balance;
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(number, balance, amount);
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
