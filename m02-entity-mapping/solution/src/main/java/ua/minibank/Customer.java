package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
public class Customer {

    /**
     * SEQUENCE + allocationSize = 50: Hibernate бере з бази одне значення послідовності
     * і видає 50 id з пам'яті. Це дозволяє вставляти рядки пачками (JDBC batching).
     * IDENTITY так не вміє: id стає відомим лише після INSERT кожного рядка.
     */
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

    /** STRING: у базі "ACTIVE". ORDINAL зберіг би 0 і зламався б після зміни порядку констант. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus status;

    /** Момент часу без прив'язки до зони. У PostgreSQL: timestamp with time zone. */
    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    /** Календарна дата без часу: день народження однаковий у будь-якому часовому поясі. */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Embedded
    private Address address;

    protected Customer() {
    }

    public Customer(String firstName, String lastName, String email, Address address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.address = address;
        this.status = CustomerStatus.ACTIVE;
        this.registeredAt = Instant.now();
    }

    /** Не зберігається в базі: Hibernate мапить поля, а не методи (бо @Id стоїть на полі). */
    public String getFullName() {
        return firstName + " " + lastName;
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

    public CustomerStatus getStatus() {
        return status;
    }

    public void block() {
        this.status = CustomerStatus.BLOCKED;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Customer{id=" + id + ", name='" + getFullName() + "', status=" + status + ", address=" + address + "}";
    }
}
