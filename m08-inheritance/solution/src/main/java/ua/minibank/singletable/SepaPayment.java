package ua.minibank.singletable;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("SEPA")
public class SepaPayment extends Payment {

    @Column(name = "beneficiary_iban", length = 34)
    private String beneficiaryIban;

    @Column(name = "beneficiary_name", length = 140)
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
