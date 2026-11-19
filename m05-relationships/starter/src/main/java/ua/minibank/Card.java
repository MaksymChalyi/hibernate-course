package ua.minibank;

import java.time.LocalDate;

/*
 * Домашнє завдання 5. Картка, прив'язана до рахунку: один рахунок — одна картка (one-to-one).
 *
 * Таблиця cards:
 *   id             bigint        первинний ключ, послідовність card_seq
 *   account_id     bigint        NOT NULL, UNIQUE, зовнішній ключ на accounts.id
 *   masked_number  varchar(19)   NOT NULL  ("4441 **** **** 1234" — повний номер картки банк не зберігає)
 *   expiry_date    date          NOT NULL
 *
 * Кроки:
 *   5.1 @Entity, @Table(name = "cards"), id з послідовності card_seq (як у Account).
 *   5.2 Над account:
 *           @OneToOne(fetch = FetchType.LAZY, optional = false)
 *           @JoinColumn(name = "account_id", nullable = false, unique = true)
 *       unique = true — саме воно гарантує на рівні бази, що в рахунку не буде двох карток.
 *   5.3 @Column(name = "masked_number", nullable = false, length = 19) і @Column(name = "expiry_date", nullable = false).
 *   5.4 protected Card() { }
 *   5.5 Розкоментуй <class>ua.minibank.Card</class> у persistence.xml і рядок з em.persist(new Card(...)) у Main.
 *
 * Перевірка: тести «5 (ДЗ) · …».
 * Питання: чому ми НЕ додаємо в Account поле Card card (зворотну сторону)? Відповідь — у лекції, резерв.
 */
// TODO 5 (ДЗ) Зробити Card entity з @OneToOne до Account (кроки 5.1–5.5 над класом)
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
