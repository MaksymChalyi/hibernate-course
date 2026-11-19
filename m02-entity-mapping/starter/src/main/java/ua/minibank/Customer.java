package ua.minibank;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Клієнт банку. Завдання 1 — замапити клас на таблицю customers.
 *
 * Яка таблиця має вийти (Hibernate створить її сам з анотацій, hbm2ddl.auto = create):
 *
 *   Поле Java                Колонка             Тип у PostgreSQL               Обмеження
 *   ---------------------    ----------------    ---------------------------    -----------------
 *   Long id                  id                  bigint                         первинний ключ, з послідовності customer_seq
 *   String firstName         first_name          varchar(100)                   NOT NULL
 *   String lastName          last_name           varchar(100)                   NOT NULL
 *   String email             email               varchar(255)                   NOT NULL, UNIQUE
 *   CustomerStatus status    status              varchar(20)                    NOT NULL, зберігається текстом: 'ACTIVE'
 *   Instant registeredAt     registered_at       timestamp with time zone       NOT NULL, не змінюється після INSERT
 *   LocalDate birthDate      birth_date          date
 *   Address address         city, street, postal_code — колонки адреси лежать у цій самій таблиці
 *
 * Порядок кроків: 1.1 → 1.9. Після кожних 2–3 кроків запускай Main і дивись create table у консолі.
 */
// TODO 1.1 Познач клас як entity для таблиці customers: @Entity і @Table(name = "customers")
public class Customer {

    // TODO 1.2 Id з послідовності (SEQUENCE) з allocationSize = 50 — три анотації, готовий код нижче
    //      @Id
    //      @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
    //      @SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq", allocationSize = 50)
    //
    //      generator = "customer_seq"       — ім'я генератора всередині Java (зв'язує дві анотації);
    //      sequenceName = "customer_seq"    — ім'я послідовності в базі;
    //      allocationSize = 50              — Hibernate бере з бази одне число і сам видає 50 id з пам'яті.
    //      Чому не IDENTITY, як у модулі 1, — у лекції, розділ «Генерація id».
    private Long id;

    // TODO 1.3 Імена: @Column(name = "first_name", nullable = false, length = 100) і так само для last_name
    //      length = 100 → varchar(100). Без length Hibernate створить varchar(255).
    private String firstName;
    private String lastName;

    // TODO 1.4 Email: @Column(nullable = false, unique = true)
    //      name можна не писати: поле email → колонка email (імена збігаються).
    private String email;

    // TODO 1.5 Статус текстом: @Enumerated(EnumType.STRING) і @Column(nullable = false, length = 20)
    //      Без @Enumerated(STRING) у базу піде НОМЕР константи (ACTIVE = 0, BLOCKED = 1).
    //      Тест «1.5 · статус зберігається текстом» перевіряє, що в колонці лежить 'ACTIVE'.
    private CustomerStatus status;

    // TODO 1.6 Дата реєстрації: @Column(name = "registered_at", nullable = false, updatable = false)
    //      updatable = false — Hibernate ніколи не включить цю колонку в UPDATE.
    //      Дату реєстрації задаємо один раз у конструкторі і більше не змінюємо.
    private Instant registeredAt;

    // TODO 1.7 Дата народження: @Column(name = "birth_date")
    //      Без name колонка називалась би birthdate: «чистий» Hibernate НЕ перетворює camelCase на snake_case сам.
    private LocalDate birthDate;

    // TODO 1.8 Вбудувати адресу: @Embedded (а клас Address позначити @Embeddable — завдання 2)
    //      Окремої таблиці для адреси не буде: колонки city, street, postal_code з'являться в customers.
    private Address address;

    // TODO 1.9 Додай конструктор без аргументів для Hibernate: protected Customer() { }

    public Customer(String firstName, String lastName, String email, Address address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.address = address;
        this.status = CustomerStatus.ACTIVE;
        this.registeredAt = Instant.now();
    }

    /**
     * Питання до тебе: чи з'явиться в таблиці колонка full_name? Подумай, потім перевір у create table.
     * Підказка: Hibernate мапить поля чи методи?
     */
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
