package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Рахунок БЕЗ захисту від паралельних змін. Існує лише для того,
 * щоб відтворити lost update. У реальному коді так робити не можна.
 */
@Entity
@Table(name = "legacy_accounts")
public class LegacyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "legacy_account_seq")
    @SequenceGenerator(name = "legacy_account_seq", sequenceName = "legacy_account_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    protected LegacyAccount() {
    }

    public LegacyAccount(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
