package ua.minibank.singletable;

import ua.minibank.common.BaseEntity;
import ua.minibank.common.CurrencyCode;
import ua.minibank.common.PaymentStatus;

import java.math.BigDecimal;

/*
 * TODO 2. SINGLE_TABLE:
 *   - @Entity, таблиця payments;
 *   - @Inheritance(strategy = InheritanceType.SINGLE_TABLE);
 *   - колонка-дискримінатор payment_type (довжина 10);
 *   - поля: amount (19,4), currency і status як текст, payer_account (34) — усі not null.
 */
public abstract class Payment extends BaseEntity {

    private BigDecimal amount;

    private CurrencyCode currency;

    private PaymentStatus status;

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
