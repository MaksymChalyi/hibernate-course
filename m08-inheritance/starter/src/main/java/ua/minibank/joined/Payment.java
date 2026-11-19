package ua.minibank.joined;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import ua.minibank.common.BaseEntity;
import ua.minibank.common.CurrencyCode;
import ua.minibank.common.PaymentStatus;

import java.math.BigDecimal;

/**
 * JOINED: спільні поля в payments, специфічні — в card_payments і sepa_payments.
 * Рядок нащадка має той самий id, що й рядок у payments (primary key = foreign key).
 *
 * + нормалізована схема, NOT NULL на полях нащадків;
 * − кожне завантаження CardPayment — JOIN двох таблиць, поліморфний запит — LEFT JOIN усіх;
 *   INSERT нащадка — два INSERT.
 */
/*
 * Завдання 5. Стратегія JOINED: спільні поля в payments, специфічні — в окремих таблицях нащадків.
 *
 *   @Entity
 *   @Table(name = "payments")
 *   @Inheritance(strategy = InheritanceType.JOINED)
 *
 * Дискримінатор не потрібен: тип видно з того, у якій таблиці нащадка є рядок з цим id.
 */
// TODO 5 JOINED: над класом @Entity, @Table(name = "payments"), @Inheritance(JOINED)
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
