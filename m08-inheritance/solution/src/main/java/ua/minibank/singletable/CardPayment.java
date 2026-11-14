package ua.minibank.singletable;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("CARD")
public class CardPayment extends Payment {

    /** Без nullable = false: у рядках SEPA-платежів тут буде NULL. */
    @Column(name = "card_mask", length = 19)
    private String cardMask;

    @Column(name = "merchant", length = 100)
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
