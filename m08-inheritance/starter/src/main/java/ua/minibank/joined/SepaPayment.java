package ua.minibank.joined;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

/*
 * Завдання 7. SEPA-платіж у JOINED: таблиця sepa_payments, первинний ключ payment_id.
 */
// TODO 7 Над класом @Entity, @Table(name = "sepa_payments"), @PrimaryKeyJoinColumn(name = "payment_id")
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
