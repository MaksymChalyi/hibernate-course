package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
    @SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq", allocationSize = 50)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    /*
     * Завдання 2. Зворотна сторона: «клієнт → його рахунки» (one-to-many).
     *
     * Колонка вже є: accounts.customer_id (завдання 1). Нова колонка НЕ потрібна.
     * Треба лише сказати Hibernate: «цей список — дзеркало поля Account.customer».
     *
     *   @OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
     *
     *   mappedBy = "customer" — ім'я ПОЛЯ в класі Account, яке керує зв'язком.
     *                           Без mappedBy Hibernate вирішить, що це окремий зв'язок, і створить
     *                           зайву таблицю customers_accounts (тест «2 · немає зайвої таблиці»).
     *   cascade PERSIST       — persist(клієнт) збереже і нові рахунки з його списку.
     *   cascade MERGE         — merge(клієнт) пройде і по рахунках.
     *   Чому БЕЗ REMOVE: у банку рахунок з історією операцій не видаляють разом з клієнтом.
     *
     * Кроки:
     *   1. прибери @Transient;
     *   2. додай @OneToMany(...) як вище.
     */
    // TODO 2 Замість @Transient: @OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Transient
    private List<Account> accounts = new ArrayList<>();

    protected Customer() {
    }

    public Customer(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /*
     * Завдання 3. Helper, який тримає ОБИДВІ сторони зв'язку синхронними.
     *
     * Зараз рахунок потрапляє лише в список accounts. Але Hibernate записує customer_id
     * за полем Account.customer (owning side), а список — лише «дзеркало».
     * Наслідок: у базу піде customer_id = NULL → помилка NOT NULL.
     *
     * Додай другий рядок:   account.setCustomer(this);
     * (setCustomer у Account — package-private: змінювати зв'язок можна лише через addAccount.)
     *
     * Перевірка: тест «3 · addAccount заповнює обидві сторони».
     */
    public void addAccount(Account account) {
        accounts.add(account);
        // TODO 3 Встановити owning side: account.setCustomer(this)
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    @Override
    public String toString() {
        return "Customer{id=" + id + ", name='" + getFullName() + "'}";
    }
}
