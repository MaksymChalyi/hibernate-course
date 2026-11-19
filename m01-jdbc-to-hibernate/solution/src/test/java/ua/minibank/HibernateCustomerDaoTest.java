package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тести завдань 1–6. Номер на початку назви тесту = номер завдання.
 * Запуск: правою кнопкою на класі → Run 'HibernateCustomerDaoTest'.
 *
 * Якщо падають УСІ тести з однаковою помилкою — проблема в налаштуванні (завдання 1–2),
 * а не в конкретному методі. Читай текст помилки в першому тесті.
 */
@DisplayName("Модуль 1 · Hibernate DAO")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
        if (emf != null) {
            emf.close();
        }
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
    @Order(1)
    @DisplayName("1–2 · Hibernate знає entity Customer (анотації + <class> у persistence.xml)")
    void customerIsRegisteredAsEntity() {
        assertDoesNotThrow(() -> emf.getMetamodel().entity(Customer.class),
                "Customer не є entity: перевір @Entity (завдання 1.1) і <class>ua.minibank.Customer</class> (завдання 2.1)");
    }

    @Test
    @Order(2)
    @DisplayName("3 · save() встановлює клієнту id")
    void saveAssignsId() {
        Customer saved = dao.save(new Customer("Olena", "Koval", "olena@example.com"));

        assertNotNull(saved.getId(), "Після save у клієнта має з'явитися id. Перевір @Id і @GeneratedValue (завдання 1.3)");
    }

    @Test
    @Order(3)
    @DisplayName("3 · save() справді записує рядок у таблицю")
    void saveWritesRowToTable() {
        dao.save(new Customer("Olena", "Koval", "olena@example.com"));

        long rows = new JdbcCustomerDao().count();
        assertEquals(1, rows, "У таблиці customers має бути 1 рядок. Чи викликається tx.commit()?");
    }

    @Test
    @Order(4)
    @DisplayName("4 · findById() знаходить збереженого клієнта")
    void findByIdReturnsSavedCustomer() {
        Customer saved = dao.save(new Customer("Ivan", "Petrenko", "ivan@example.com"));

        Customer found = dao.findById(saved.getId()).orElseThrow();

        assertEquals("Ivan", found.getFirstName());
        assertEquals("ivan@example.com", found.getEmail(), "Неправильний email: перевір @Column над полем email (завдання 1.4)");
    }

    @Test
    @Order(5)
    @DisplayName("4 · findById() повертає Optional.empty() для неіснуючого id")
    void findByIdReturnsEmptyForUnknownId() {
        assertTrue(dao.findById(999L).isEmpty(), "em.find повертає null — загорни його в Optional.ofNullable");
    }

    @Test
    @Order(6)
    @DisplayName("5 · findAll() повертає всіх клієнтів у порядку id")
    void findAllReturnsCustomersOrderedById() {
        dao.save(new Customer("Olena", "Koval", "olena@example.com"));
        dao.save(new Customer("Ivan", "Petrenko", "ivan@example.com"));

        List<Customer> all = dao.findAll();

        assertEquals(2, all.size());
        assertEquals("Olena", all.get(0).getFirstName(), "Першою має бути Olena: чи є 'order by c.id' у запиті?");
        assertEquals("Ivan", all.get(1).getFirstName());
    }

    @Test
    @Order(7)
    @DisplayName("6 · deleteById() видаляє клієнта")
    void deleteByIdRemovesCustomer() {
        Customer saved = dao.save(new Customer("Maria", "Shevchenko", "maria@example.com"));

        dao.deleteById(saved.getId());

        assertTrue(dao.findById(saved.getId()).isEmpty(), "Клієнт досі в базі: чи є tx.commit() після em.remove?");
    }

    @Test
    @Order(8)
    @DisplayName("6 · deleteById() не падає для неіснуючого id")
    void deleteByIdIgnoresUnknownId() {
        assertDoesNotThrow(() -> dao.deleteById(999L), "Якщо клієнта немає, em.find повертає null — перевір на null перед em.remove");
    }

    @Test
    @Order(9)
    @DisplayName("Бонус · Hibernate бачить рядки, вставлені через JDBC (таблиця та сама)")
    void hibernateSeesRowsInsertedByJdbc() {
        Customer viaJdbc = new JdbcCustomerDao().save(new Customer("Taras", "Bondar", "taras@example.com"));

        Customer viaHibernate = dao.findById(viaJdbc.getId()).orElseThrow();

        assertEquals("Taras", viaHibernate.getFirstName());
    }
}
