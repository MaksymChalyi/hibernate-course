package ua.minibank;

import java.time.Instant;
import java.time.LocalDate;

/**
 * TODO 1. Зроби клас entity для таблиці customers:
 *   - id: SEQUENCE-генератор "customer_seq" з allocationSize = 50
 *         (@GeneratedValue(strategy = SEQUENCE, generator = ...) + @SequenceGenerator);
 *   - first_name, last_name: not null, довжина 100;
 *   - email: not null, unique;
 *   - status: зберігати як текст ("ACTIVE"), not null, довжина 20;
 *   - registered_at: not null, не оновлюється після вставки (updatable = false);
 *   - birth_date: звичайна колонка;
 *   - address: вбудований об'єкт (@Embedded).
 * Не забудь конструктор без аргументів.
 */
public class Customer {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private CustomerStatus status;
    private Instant registeredAt;
    private LocalDate birthDate;
    private Address address;

    public Customer(String firstName, String lastName, String email, Address address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.address = address;
        this.status = CustomerStatus.ACTIVE;
        this.registeredAt = Instant.now();
    }

    /** Питання: чи з'явиться колонка full_name у таблиці? Чому? */
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
