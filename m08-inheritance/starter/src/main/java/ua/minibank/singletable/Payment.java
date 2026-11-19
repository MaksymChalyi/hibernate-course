package ua.minibank.singletable;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
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
 * SINGLE_TABLE: уся ієрархія в одній таблиці payments.
 * Колонка payment_type (discriminator) каже Hibernate, який клас створити з рядка.
 *
 * + один SELECT без JOIN для будь-якого запиту, найшвидша стратегія;
 * − колонки нащадків мусять бути nullable: у рядку картки немає IBAN і навпаки.
 *   Цілісність даних перевіряє лише код, не база.
 */
/*
 * Завдання 2. Стратегія SINGLE_TABLE: уся ієрархія платежів — в ОДНІЙ таблиці payments.
 *
 *   @Entity
 *   @Table(name = "payments")
 *   @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
 *   @DiscriminatorColumn(name = "payment_type", length = 10)
 *
 * @DiscriminatorColumn — колонка-«мітка» типу. У рядку карткового платежу там буде 'CARD', SEPA — 'SEPA'.
 * За цією міткою Hibernate вирішує, який клас створити, читаючи рядок.
 * Колонки полів (amount, currency, ...) уже описані — додати треба лише 4 анотації над класом.
 */
// TODO 2 SINGLE_TABLE: над класом @Entity, @Table(name = "payments"), @Inheritance(SINGLE_TABLE), @DiscriminatorColumn(name = "payment_type", length = 10)
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
