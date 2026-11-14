package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Домашнє завдання: одна картка на рахунок.
 *
 * Owning side — Card: foreign key account_id лежить у таблиці cards.
 * unique = true робить зв'язок справді "один до одного" на рівні бази.
 *
 * Зворотного поля Account.card немає свідомо: inverse side @OneToOne Hibernate не може
 * завантажити ліниво (він не знає без запиту, null там чи proxy), тож кожне завантаження
 * рахунку тягнуло б додатковий SELECT по cards.
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "card_seq")
    @SequenceGenerator(name = "card_seq", sequenceName = "card_seq", allocationSize = 50)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    /** Повний номер картки банк не зберігає у відкритому вигляді (PCI DSS). */
    @Column(name = "masked_number", nullable = false, length = 19)
    private String maskedNumber;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    protected Card() {
    }

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
