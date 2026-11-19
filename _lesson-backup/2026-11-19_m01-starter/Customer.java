package ua.minibank;

import jakarta.persistence.*;

/**
 * Клієнт банку.
 * <p>
 * TODO 1: перетвори клас на entity:
 *   - @Entity і @Table(name = "customers");
 *   - @Id і @GeneratedValue(strategy = GenerationType.IDENTITY) для id;
 *   - @Column(name = "first_name", nullable = false) і так само для last_name;
 *   - email: nullable = false, unique = true;
 *   - додай конструктор без аргументів (Hibernate створює об'єкти через нього). Зроби його protected.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "last_name", nullable = false, unique = true)
    private String email;

    public Customer(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    protected Customer() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
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

    @Override
    public String toString() {
        return "Customer{id=" + id + ", name='" + firstName + " " + lastName + "', email='" + email + "'}";
    }
}
