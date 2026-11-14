package ua.minibank;

import java.util.Objects;

/**
 * TODO 2. Зроби клас вбудованим (@Embeddable):
 *   - колонки city (довжина 100), street, postal_code (довжина 10);
 *   - конструктор без аргументів.
 * У Address немає id: це value object, його колонки лежать у таблиці customers.
 */
public class Address {

    private String city;
    private String street;
    private String postalCode;

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
