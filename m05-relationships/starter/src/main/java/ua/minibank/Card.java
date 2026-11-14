package ua.minibank;

import java.time.LocalDate;

/**
 * Домашнє завдання. TODO 5: зроби Card entity для таблиці cards.
 *   - id: SEQUENCE "card_seq";
 *   - account: @OneToOne, LAZY, обов'язковий, колонка account_id. Яка властивість @JoinColumn
 *     гарантує, що в рахунку не буде двох карток?
 *   - masked_number (довжина 19), expiry_date — not null;
 *   - конструктор без аргументів;
 *   - розкоментуй Card у persistence.xml і рядок з картою в Main.
 *
 * Питання: чому ми НЕ додаємо поле Account.card (зворотну сторону)?
 */
public class Card {

    private Long id;
    private Account account;
    private String maskedNumber;
    private LocalDate expiryDate;

    public Card(Account account, String maskedNumber, LocalDate expiryDate) {
        this.account = account;
        this.maskedNumber = maskedNumber;
        this.expiryDate = expiryDate;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public String getMaskedNumber() {
        return maskedNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }
}
