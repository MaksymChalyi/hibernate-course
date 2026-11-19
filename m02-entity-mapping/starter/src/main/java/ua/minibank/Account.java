package ua.minibank;

import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Банківський рахунок. Завдання 3 — замапити його на таблицю accounts.
 *
 *   Поле Java              Колонка    Тип у PostgreSQL     Обмеження
 *   -------------------    -------    ----------------     ---------------------------------------------
 *   Long id                id         bigint               первинний ключ, послідовність account_seq
 *   String number          number     varchar(34)          NOT NULL, UNIQUE (IBAN — до 34 символів)
 *   Currency currency      currency   varchar(3)           NOT NULL ('UAH', 'USD', 'EUR')
 *   BigDecimal balance     balance    numeric(19,4)        NOT NULL — 19 цифр усього, 4 після коми
 *
 * Чому гроші в BigDecimal, а не в double, — у лекції, розділ «Гроші». Коротко: 0.1 + 0.2 != 0.3 у double.
 */
// TODO 3.1 @Entity і @Table(name = "accounts")
public class Account {

    // TODO 3.2 Id з послідовності account_seq, allocationSize = 50 (як у Customer, крок 1.2, тільки інші імена)
    private Long id;

    // TODO 3.3 Номер рахунку: @Column(nullable = false, unique = true, length = 34)
    private String number;

    // TODO 3.4 Валюта: @Column(nullable = false, length = 3)
    //      Hibernate 6 сам вміє зберігати java.util.Currency як текст 'UAH'.
    private Currency currency;

    // TODO 3.5 Баланс: @Column(nullable = false, precision = 19, scale = 4)
    //      precision = 19 — усього цифр; scale = 4 — з них після коми. Виходить numeric(19,4).
    //      Без precision/scale Hibernate створить numeric(38,2): лише 2 знаки після коми.
    private BigDecimal balance;

    /*
     * Завдання 4 (резерв). Кредитний ліміт — сума разом із валютою (клас Money).
     * Зараз поле позначене @Transient — «не зберігати в базі», щоб Hibernate не падав, поки Money не готовий.
     *
     * Коли зробиш Money вбудованим (@Embeddable, завдання 4.1 у Money.java):
     *   1. прибери @Transient;
     *   2. додай @Embedded;
     *   3. перейменуй колонки, щоб вони не збігалися з полями currency/balance цього класу:
     *
     *      @AttributeOverrides({
     *          @AttributeOverride(name = "amount",   column = @Column(name = "credit_limit_amount", precision = 19, scale = 4)),
     *          @AttributeOverride(name = "currency", column = @Column(name = "credit_limit_currency", length = 3))
     *      })
     *
     *   name = "amount" — ім'я ПОЛЯ в класі Money; column = ... — нова колонка в accounts.
     *   4. розкоментуй <class>ua.minibank.Money</class> у persistence.xml.
     * Перевірка: тест «4 · кредитний ліміт у колонках credit_limit_*».
     */
    // TODO 4.2 (резерв) Замапити creditLimit як @Embedded з колонками credit_limit_amount і credit_limit_currency
    @Transient
    private Money creditLimit;

    // TODO 3.6 Конструктор без аргументів для Hibernate: protected Account() { }

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
