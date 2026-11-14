package ua.minibank.domain;

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
import java.time.LocalDate;

/** Рядок зовнішньої банківської виписки (модуль 10). */
@Entity
@Table(name = "statement_lines")
public class StatementLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "statement_line_seq")
    @SequenceGenerator(name = "statement_line_seq", sequenceName = "statement_line_seq", allocationSize = 50)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 34)
    private String accountNumber;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

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

    /** Формат: account_number;booking_date;amount;currency;description */
    public static StatementLine parse(String line, long lineNumber) {
        String[] p = line.split(";", -1);
        if (p.length != 5) {
            throw new IllegalArgumentException("Рядок " + lineNumber + ": очікували 5 полів, а маємо " + p.length);
        }
        try {
            return new StatementLine(p[0], LocalDate.parse(p[1]), new BigDecimal(p[2]), CurrencyCode.valueOf(p[3]), p[4]);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Рядок " + lineNumber + ": " + e.getMessage(), e);
        }
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
