package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingTest {

    private static final Currency UAH = Currency.getInstance("UAH");
    private static EntityManagerFactory emf;

    @BeforeAll
    static void start() {
        emf = Persistence.createEntityManagerFactory("minibank");
    }

    @AfterAll
    static void stop() {
        emf.close();
    }

    @BeforeEach
    void clean() {
        inTransaction(em -> em.createNativeQuery("truncate table customers, accounts").executeUpdate());
    }

    // ---------- Обов'язкова частина ----------

    @Test
    void customerWithAddressIsSavedAndLoaded() {
        Customer olena = new Customer("Olena", "Koval", "olena@example.com",
                new Address("Kyiv", "Khreshchatyk 1", "01001"));
        inTransaction(em -> em.persist(olena));

        try (EntityManager em = emf.createEntityManager()) {
            Customer loaded = em.find(Customer.class, olena.getId());
            assertEquals("Kyiv", loaded.getAddress().getCity());
            assertEquals(CustomerStatus.ACTIVE, loaded.getStatus());
            assertTrue(loaded.getRegisteredAt() != null);
        }
    }

    @Test
    void addressColumnsLiveInCustomersTable() {
        inTransaction(em -> em.persist(new Customer("Olena", "Koval", "olena@example.com",
                new Address("Lviv", "Rynok 1", "79000"))));

        try (EntityManager em = emf.createEntityManager()) {
            Object city = em.createNativeQuery("select city from customers").getSingleResult();
            assertEquals("Lviv", city);
        }
    }

    @Test
    void statusIsStoredAsText() {
        inTransaction(em -> em.persist(new Customer("Ivan", "Petrenko", "ivan@example.com", null)));

        try (EntityManager em = emf.createEntityManager()) {
            Object status = em.createNativeQuery("select status from customers").getSingleResult();
            assertEquals("ACTIVE", status, "EnumType.STRING зберігає ім'я константи");
        }
    }

    @Test
    void customerIdComesFromSequenceWithAllocationSize50() {
        try (EntityManager em = emf.createEntityManager()) {
            Object incrementBy = em.createNativeQuery(
                    "select increment_by from pg_sequences where schemaname = 'm02' and sequencename = 'customer_seq'")
                    .getSingleResult();
            assertEquals(50L, ((Number) incrementBy).longValue());
        }
    }

    @Test
    void balanceKeepsFourDigitsAfterPoint() {
        Account account = new Account("UA001", UAH, new BigDecimal("1234.5"));
        inTransaction(em -> em.persist(account));

        try (EntityManager em = emf.createEntityManager()) {
            Object raw = em.createNativeQuery("select cast(balance as text) from accounts").getSingleResult();
            assertEquals("1234.5000", raw);

            Account loaded = em.find(Account.class, account.getId());
            // compareTo, бо new BigDecimal("1234.5").equals(new BigDecimal("1234.5000")) == false
            assertEquals(0, new BigDecimal("1234.5").compareTo(loaded.getBalance()));
        }
    }

    @Test
    void doubleCannotStoreMoneyExactly() {
        assertNotEquals(0.3, 0.1 + 0.2);
        assertEquals(new BigDecimal("0.3"), new BigDecimal("0.1").add(new BigDecimal("0.2")));
    }

    @Test
    void requiredFieldIsChecked() {
        Customer withoutLastName = new Customer("Taras", null, "taras@example.com", null);

        assertThrows(PersistenceException.class, () -> inTransaction(em -> em.persist(withoutLastName)));
    }

    // ---------- Резерв і домашнє завдання ----------

    @Test
    void currencyIsStoredAsIsoCode() {
        inTransaction(em -> em.persist(new Account("UA002", Currency.getInstance("USD"), BigDecimal.TEN)));

        try (EntityManager em = emf.createEntityManager()) {
            Object currency = em.createNativeQuery("select currency from accounts").getSingleResult();
            assertEquals("USD", currency);
        }
    }

    @Test
    void creditLimitUsesOverriddenColumns() {
        Account account = new Account("UA003", UAH, BigDecimal.ZERO);
        account.setCreditLimit(Money.of("20000", "UAH"));
        inTransaction(em -> em.persist(account));

        try (EntityManager em = emf.createEntityManager()) {
            Object[] row = (Object[]) em.createNativeQuery(
                    "select cast(credit_limit_amount as text), credit_limit_currency from accounts").getSingleResult();
            assertEquals("20000.0000", row[0]);
            assertEquals("UAH", row[1]);

            Account loaded = em.find(Account.class, account.getId());
            assertEquals(Money.of("20000", "UAH"), loaded.getCreditLimit());
        }
    }

    private static void inTransaction(Consumer<EntityManager> work) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                work.accept(em);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }
}
