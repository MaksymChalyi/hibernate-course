package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HibernateCustomerDaoTest {

    private static EntityManagerFactory emf;
    private HibernateCustomerDao dao;

    @BeforeAll
    static void startHibernate() {
        new JdbcCustomerDao().createTableIfNotExists();
        emf = Persistence.createEntityManagerFactory("minibank");
    }

    @AfterAll
    static void stopHibernate() {
        emf.close();
    }

    @BeforeEach
    void cleanTable() {
        dao = new HibernateCustomerDao(emf);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("truncate table customers restart identity").executeUpdate();
            em.getTransaction().commit();
        }
    }

    @Test
    void saveAssignsId() {
        Customer saved = dao.save(new Customer("Olena", "Koval", "olena@example.com"));

        assertNotNull(saved.getId(), "Після save у клієнта має з'явитися id");
    }

    @Test
    void findByIdReturnsSavedCustomer() {
        Customer saved = dao.save(new Customer("Ivan", "Petrenko", "ivan@example.com"));

        Customer found = dao.findById(saved.getId()).orElseThrow();

        assertEquals("Ivan", found.getFirstName());
        assertEquals("ivan@example.com", found.getEmail());
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertTrue(dao.findById(999L).isEmpty());
    }

    @Test
    void findAllReturnsCustomersOrderedById() {
        dao.save(new Customer("Olena", "Koval", "olena@example.com"));
        dao.save(new Customer("Ivan", "Petrenko", "ivan@example.com"));

        List<Customer> all = dao.findAll();

        assertEquals(2, all.size());
        assertEquals("Olena", all.get(0).getFirstName());
        assertEquals("Ivan", all.get(1).getFirstName());
    }

    @Test
    void deleteByIdRemovesCustomer() {
        Customer saved = dao.save(new Customer("Maria", "Shevchenko", "maria@example.com"));

        dao.deleteById(saved.getId());

        assertTrue(dao.findById(saved.getId()).isEmpty());
    }

    @Test
    void hibernateSeesRowsInsertedByJdbc() {
        Customer viaJdbc = new JdbcCustomerDao().save(new Customer("Taras", "Bondar", "taras@example.com"));

        Customer viaHibernate = dao.findById(viaJdbc.getId()).orElseThrow();

        assertEquals("Taras", viaHibernate.getFirstName());
    }
}
