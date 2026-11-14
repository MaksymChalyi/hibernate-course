package ua.minibank.joined;

import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

/*
 * TODO 7. @Entity, таблиця sepa_payments, первинний ключ payment_id. Поля not null.
 */
public class SepaPayment extends Payment {

    private String beneficiaryIban;

    private String beneficiaryName;

    protected SepaPayment() {
    }

    public SepaPayment(BigDecimal amount, CurrencyCode currency, String payerAccount,
                       String beneficiaryIban, String beneficiaryName) {
        super(amount, currency, payerAccount);
        this.beneficiaryIban = beneficiaryIban;
        this.beneficiaryName = beneficiaryName;
    }

    public String getBeneficiaryIban() {
        return beneficiaryIban;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }
}
