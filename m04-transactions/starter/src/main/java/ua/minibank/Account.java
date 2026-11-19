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

    protected Account() {
    }

    public Account(String number, CurrencyCode currency, BigDecimal balance) {
        this.number = number;
        this.currency = currency;
        this.balance = balance;
    }

    /*
     * Завдання 2a (ДЗ). Не дозволити піти в мінус.
     *
     * Якщо balance < amount → throw new InsufficientFundsException(number, balance, amount);
     * Порівнювати BigDecimal — тільки через compareTo:
     *     balance.compareTo(amount) < 0     означає    balance < amount
     *
     * Питання: чому цю перевірку краще робити тут, в Account, а не в TransferService?
     * (Підказка: хто ще в майбутньому може списувати гроші з рахунку?)
     */
    public void debit(BigDecimal amount) {
        // TODO 2a (ДЗ) Кинути InsufficientFundsException, якщо грошей не вистачає
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

    @Override
    public String toString() {
        return "Account{" + number + ", " + balance.toPlainString() + " " + currency + "}";
    }
}
