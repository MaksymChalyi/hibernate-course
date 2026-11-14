package ua.minibank.singletable;

import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

/*
 * TODO 4. @Entity, значення дискримінатора "SEPA". Колонки beneficiary_iban (34), beneficiary_name (140).
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
