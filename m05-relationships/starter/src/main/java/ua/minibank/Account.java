package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq")
    @SequenceGenerator(name = "account_seq", sequenceName = "account_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, length = 34)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /*
     * Завдання 1. Зв'язок «рахунок → клієнт» (many-to-one: багато рахунків в одного клієнта).
     *
     * У базі це одна колонка accounts.customer_id — зовнішній ключ (foreign key) на customers.id.
     * У Java — поле типу Customer. Hibernate сам запише в customer_id значення customer.getId().
     *
     *   @ManyToOne(fetch = FetchType.LAZY, optional = false)
     *   @JoinColumn(name = "customer_id", nullable = false)
     *
     *   @ManyToOne           — «багато рахунків — до одного клієнта».
     *   fetch = LAZY         — не завантажувати клієнта разом з рахунком, а лише коли звернемось до нього.
     *                          За замовчуванням @ManyToOne завантажує одразу (EAGER) — детально в модулі 6.
     *   optional = false     — рахунок без клієнта не існує.
     *   @JoinColumn          — ім'я колонки зовнішнього ключа. Без неї Hibernate назвав би її customer_id сам,
     *                          але явно — зрозуміліше.
     *
     * Cascade тут НЕ потрібен. Подумай: що означало б CascadeType.REMOVE на цьому полі?
     * (Тест «1 · видалення рахунку не видаляє клієнта» це перевіряє.)
     */
    // TODO 1 Над полем customer: @ManyToOne(fetch = FetchType.LAZY, optional = false) і @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /*
     * Питання для обговорення: чому тут НЕМАЄ списку List<Transaction> transactions?
     * (Підказка: скільки операцій буде на рахунку через 5 років? Відповідь — у лекції, частина 4.)
     */

    protected Account() {
    }

    public Account(String number, CurrencyCode currency, BigDecimal balance) {
        this.number = number;
        this.currency = currency;
        this.balance = balance;
    }

    void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Customer getCustomer() {
        return customer;
    }

    @Override
    public String toString() {
        return "Account{" + number + ", " + balance.toPlainString() + " " + currency + "}";
    }
}
