package ua.minibank.joined;

import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

/*
 * TODO 6. @Entity, таблиця card_payments, первинний ключ payment_id (@PrimaryKeyJoinColumn).
 * Тут поля можуть бути not null. Чому?
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
