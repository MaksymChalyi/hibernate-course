package ua.minibank;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

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
     * Inverse side: mappedBy = "customer" каже Hibernate, що foreign key записує
     * поле Account.customer. Ця колекція на SQL не впливає, вона лише для навігації.
     *
     * cascade PERSIST і MERGE: новий клієнт зберігається разом із новими рахунками.
     * REMOVE і orphanRemoval свідомо не додані: у банку рахунок закривають, а не видаляють,
     * і видалення клієнта не повинно мовчки видаляти рахунки з історією операцій.
     */
    @OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Account> accounts = new ArrayList<>();

    protected Customer() {
    }

    public Customer(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /**
     * Helper тримає обидві сторони зв'язку синхронними.
     * Якщо додати рахунок лише в список, customer_id у базі буде порожнім:
     * Hibernate дивиться на owning side (Account.customer), а не на цей список.
     */
    public void addAccount(Account account) {
        accounts.add(account);
        account.setCustomer(this);
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

    /** Назовні — лише для читання, щоб ніхто не обійшов addAccount. */
    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    @Override
    public String toString() {
        // Без accounts: інакше toString завантажить колекцію (зайвий SELECT або LazyInitializationException)
        return "Customer{id=" + id + ", name='" + getFullName() + "'}";
    }
}
