package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Рядок банківської виписки, імпортований з CSV.
 */
@Entity
@Table(name = "statement_lines")
public class StatementLine {

    /*
     * Завдання 1. Генерація id, з якою працює пакетна вставка (batching).
     *
     * Зараз IDENTITY. З ним Hibernate мусить виконати INSERT одразу на кожен persist — щоб дізнатися id.
     * Тому навіть з hibernate.jdbc.batch_size пакетів не буде: 100 000 рядків = 100 000 окремих запитів.
     *
     * Заміни дві анотації нижче на (як у модулі 2):
     *     @Id
     *     @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "statement_line_seq")
     *     @SequenceGenerator(name = "statement_line_seq", sequenceName = "statement_line_seq", allocationSize = 50)
     *
     * Після цього Emfs.createSchema() створить послідовність, а JdbcBatchImporter (він бере nextval('statement_line_seq'))
     * теж запрацює.
     */
    // TODO 1 Замінити IDENTITY на SEQUENCE "statement_line_seq" з allocationSize = 50
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 34)
    private String accountNumber;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    /** Зі знаком: мінус — списання, плюс — зарахування. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(nullable = false)
    private String description;

    protected StatementLine() {
    }

    public StatementLine(String accountNumber, LocalDate bookingDate, BigDecimal amount,
                         CurrencyCode currency, String description) {
        this.accountNumber = accountNumber;
        this.bookingDate = bookingDate;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }
}
