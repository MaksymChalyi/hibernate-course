package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.Objects;

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

    protected Customer() {
    }

    public Customer(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
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

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /*
     * Завдання 4 (резерв). equals/hashCode для entity.
     *
     * Ця реалізація виглядає логічно («рівні, якщо однаковий id»), але має дві помилки.
     * Знайди їх через два тести в CustomerServiceTest:
     *
     *   «4 · два нових клієнти не рівні» — у нових клієнтів id = null.
     *        Objects.equals(null, null) == true → два РІЗНІ нові клієнти вважаються рівними.
     *
     *   «4 · клієнт не губиться в HashSet після persist» — hashCode залежить від id.
     *        Поклали клієнта в HashSet, коли id = null → кошик за hashCode(null).
     *        Після persist id = 1 → hashCode інший → set.contains(...) шукає в іншому кошику і не знаходить.
     *
     * Що зробити:
     *   equals:   рівні лише ті, у кого id НЕ null і однаковий.
     *             Використовуй getId(), а не поле id — у proxy Hibernate (модуль 6) поле порожнє.
     *             instanceof, а не getClass() — proxy є підкласом Customer.
     *   hashCode: має НЕ змінюватися, коли з'являється id. Найпростіше — константа:
     *             return Customer.class.hashCode();
     * Розбір варіантів — у лекції, частина «equals і hashCode».
     */
    // TODO 4 (резерв) Виправити equals (null-id не рівні) і hashCode (не залежить від id)
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Customer other)) {
            return false;
        }
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Customer{id=" + id + ", name='" + firstName + " " + lastName + "'}";
    }
}
