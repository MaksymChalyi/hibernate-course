package ua.minibank;

import jakarta.persistence.EntityManager;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerServiceTest {

    private final CustomerService service = new CustomerService();
    private Long customerId;

    @BeforeEach
    void prepare() {
        Jpa.runInTransaction(em -> em.createQuery("delete from Customer").executeUpdate());
        customerId = Jpa.inTransaction(em -> {
            Customer c = new Customer("Olena", "Koval", "olena@example.com");
            em.persist(c);
            return c.getId();
        });
    }

    // ---------- Обов'язкова частина ----------

    @Test
    void renameUsesDirtyCheckingOnly() {
        Statistics stats = Jpa.statistics();
        stats.clear();

        service.rename(customerId, "Kovalenko");

        assertEquals(2, stats.getPrepareStatementCount(), "Очікуємо рівно SELECT + UPDATE");
        assertEquals("Kovalenko", load(customerId).getLastName());
    }

    // ---------- Резерв ----------

    @Test
    void mergeReturnsManagedCopyAndSavesChanges() {
        Customer detached = load(customerId);

        Customer merged = service.changeEmail(detached, "olena.new@example.com");

        assertNotSame(detached, merged, "merge повертає інший об'єкт");
        assertEquals("olena.new@example.com", load(customerId).getEmail());
    }

    @Test
    void twoNewCustomersAreNotEqual() {
        Customer a = new Customer("Ivan", "Petrenko", "ivan@example.com");
        Customer b = new Customer("Maria", "Shevchenko", "maria@example.com");

        assertNotEquals(a, b, "Без id — це різні клієнти");
    }

    @Test
    void customerLoadedTwiceIsEqual() {
        assertEquals(load(customerId), load(customerId));
    }

    @Test
    void customerIsStillFoundInHashSetAfterPersist() {
        Customer taras = new Customer("Taras", "Bondar", "taras@example.com");
        Set<Customer> set = new HashSet<>();
        set.add(taras);

        Jpa.runInTransaction(em -> em.persist(taras));

        assertTrue(set.contains(taras), "hashCode не має змінюватись після появи id");
    }

    @Test
    void detachedObjectIsNotManaged() {
        try (EntityManager em = Jpa.emf().createEntityManager()) {
            Customer c = em.find(Customer.class, customerId);
            assertTrue(em.contains(c));
            em.detach(c);
            assertFalse(em.contains(c));
        }
    }

    private Customer load(Long id) {
        try (EntityManager em = Jpa.emf().createEntityManager()) {
            return em.find(Customer.class, id);
        }
    }
}
