package ua.minibank.tableperclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

@Entity
@Table(name = "card_payments")
public class CardPayment extends Payment {

    /** NOT NULL можливий: у таблиці card_payments лише карткові платежі. */
    @Column(name = "card_mask", nullable = false, length = 19)
    private String cardMask;

    @Column(name = "merchant", nullable = false, length = 100)
    private String merchant;

    protected CardPayment() {
    }

    public CardPayment(BigDecimal amount, CurrencyCode currency, String payerAccount, String cardMask, String merchant) {
        super(amount, currency, payerAccount);
        this.cardMask = cardMask;
        this.merchant = merchant;
    }

    public String getCardMask() {
        return cardMask;
    }

    public String getMerchant() {
        return merchant;
    }
}
