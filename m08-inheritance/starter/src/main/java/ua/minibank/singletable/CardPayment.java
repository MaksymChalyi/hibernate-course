package ua.minibank.singletable;

import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

/*
 * TODO 3. @Entity, значення дискримінатора "CARD". Колонки card_mask (19), merchant (100).
 * Питання: чи можна поставити nullable = false на card_mask? Що станеться при збереженні SepaPayment?
 */
public class CardPayment extends Payment {

    private String cardMask;

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
