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
     * TODO 1. З IDENTITY batching не працює навіть з jdbc.batch_size. Чому?
     * Заміни на SEQUENCE "statement_line_seq" з allocationSize = 50.
     */
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
