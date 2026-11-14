package ua.minibank.tableperclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import ua.minibank.common.BaseEntity;
import ua.minibank.common.CurrencyCode;
import ua.minibank.common.PaymentStatus;

import java.math.BigDecimal;

/**
 * Резерв. TABLE_PER_CLASS: у кожного конкретного класу своя повна таблиця
 * (card_payments, sepa_payments), спільні колонки повторюються в кожній.
 * Абстрактний Payment таблиці не має.
 *
 * + запит до конкретного типу — одна таблиця без JOIN;
 * − поліморфний запит "select p from Payment p" — UNION ALL усіх таблиць;
 *   id мають бути унікальні в усій ієрархії, тому IDENTITY заборонено (лише SEQUENCE/TABLE).
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Payment extends BaseEntity {

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "payer_account", nullable = false, length = 34)
    private String payerAccount;

    protected Payment() {
    }

    protected Payment(BigDecimal amount, CurrencyCode currency, String payerAccount) {
        this.amount = amount;
        this.currency = currency;
        this.payerAccount = payerAccount;
        this.status = PaymentStatus.NEW;
    }

    public void complete() {
        status = PaymentStatus.COMPLETED;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPayerAccount() {
        return payerAccount;
    }
}
