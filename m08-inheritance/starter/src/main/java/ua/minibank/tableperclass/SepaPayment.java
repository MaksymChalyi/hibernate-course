package ua.minibank.tableperclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

/*
 * Завдання 10 (резерв). @Entity і @Table(name = "sepa_payments").
 */
// TODO 10 (резерв) Над класом @Entity і @Table(name = "sepa_payments")
public class SepaPayment extends Payment {

    @Column(name = "beneficiary_iban", nullable = false, length = 34)
    private String beneficiaryIban;

    @Column(name = "beneficiary_name", nullable = false, length = 140)
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
