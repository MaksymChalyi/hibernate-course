package ua.minibank.joined;

import ua.minibank.common.BaseEntity;
import ua.minibank.common.CurrencyCode;
import ua.minibank.common.PaymentStatus;

import java.math.BigDecimal;

/*
 * TODO 5. JOINED: @Entity, таблиця payments, @Inheritance(strategy = InheritanceType.JOINED).
 * Поля — як у singletable.Payment. Дискримінатор не потрібен.
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
