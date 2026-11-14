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

    /**
     * TODO 2. Зроби зворотну сторону зв'язку "клієнт → рахунки":
     *   - прибери @Transient;
     *   - @OneToMany з mappedBy (на яке поле в Account?);
     *   - cascade: які операції мають переходити з клієнта на рахунки? Обґрунтуй вибір.
     * Питання: що станеться без mappedBy? Перевір тестом foreignKeyLivesInAccountsTableAndNoJoinTableIsCreated.
     */
    @Transient
    private List<Account> accounts = new ArrayList<>();

    protected Customer() {
    }

    public Customer(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /**
     * TODO 3. Helper має синхронізувати ОБИДВІ сторони зв'язку.
     * Зараз рахунок потрапляє лише в список. Що тоді буде в колонці customer_id?
     */
    public void addAccount(Account account) {
        accounts.add(account);
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
