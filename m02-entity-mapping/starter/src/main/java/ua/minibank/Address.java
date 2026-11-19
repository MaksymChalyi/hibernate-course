package ua.minibank;

import java.util.Objects;

/**
 * Адреса клієнта — value object («об'єкт-значення»).
 *
 * У адреси немає власного id і власної таблиці: вона існує лише як частина клієнта.
 * Тому це не @Entity, а @Embeddable — «вбудовуваний»: її поля стають колонками таблиці власника.
 *
 *   Поле Java          Колонка в customers     Тип
 *   -----------        -------------------     ------------
 *   String city        city                    varchar(100)
 *   String street      street                  varchar(255)
 *   String postalCode  postal_code             varchar(10)
 */
// TODO 2.1 Познач клас анотацією @Embeddable (не @Entity!)
public class Address {

    // TODO 2.2 Колонки адреси: @Column(length = 100) над city, @Column(name = "postal_code", length = 10) над postalCode
    //      street можна лишити без анотації: буде колонка street varchar(255).
    private String city;
    private String street;
    private String postalCode;

    // TODO 2.3 Конструктор без аргументів для Hibernate: protected Address() { }

    public Address(String city, String street, String postalCode) {
        this.city = city;
        this.street = street;
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public String getPostalCode() {
        return postalCode;
    }

    /** Value object порівнюється за значеннями всіх полів: дві однакові адреси — рівні. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Address other)) {
            return false;
        }
        return Objects.equals(city, other.city)
                && Objects.equals(street, other.street)
                && Objects.equals(postalCode, other.postalCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, street, postalCode);
    }

    @Override
    public String toString() {
        return postalCode + ", " + city + ", " + street;
    }
}
