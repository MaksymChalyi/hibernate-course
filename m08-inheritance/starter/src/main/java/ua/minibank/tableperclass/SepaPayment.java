package ua.minibank.tableperclass;

import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

/*
 * TODO 10 (резерв). @Entity, таблиця sepa_payments. Поля not null.
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
