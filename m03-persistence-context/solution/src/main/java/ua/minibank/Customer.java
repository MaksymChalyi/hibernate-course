package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

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

    /**
     * Резерв: equals/hashCode для entity зі згенерованим id.
     *
     * 1. Два нових об'єкти (id == null) не рівні між собою: це різні клієнти.
     * 2. Об'єкти з однаковим id рівні, навіть якщо завантажені різними EntityManager.
     * 3. hashCode не змінюється, коли після persist з'являється id.
     *    Інакше об'єкт "губиться" в HashSet, куди його поклали до persist.
     * 4. getId(), а не поле id: у Hibernate-proxy поле порожнє, значення повертає метод.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Customer other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        // Константа для всіх Customer. Ціна: усі клієнти в одному bucket HashSet.
        // Для колекцій із сотень елементів це прийнятно; для мільйонів — не тримайте entity в HashSet.
        return Customer.class.hashCode();
    }

    @Override
    public String toString() {
        return "Customer{id=" + id + ", name='" + firstName + " " + lastName + "'}";
    }
}
