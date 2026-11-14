package ua.minibank;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Value object: у адреси немає власного id і власної таблиці.
 * Її колонки лежать у таблиці власника (customers).
 */
@Embeddable
public class Address {

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "street")
    private String street;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    protected Address() {
    }

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

    /** Value object порівнюється за значеннями полів. */
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
