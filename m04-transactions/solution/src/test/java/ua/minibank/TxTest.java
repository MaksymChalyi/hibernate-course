package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Резерв: helper Tx. */
@DisplayName("Модуль 4 · Helper Tx (резерв)")
class TxTest {

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
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("truncate table transactions, accounts").executeUpdate();
            em.getTransaction().commit();
        }
    }

    @Test
    @DisplayName("3 (резерв) · inTransaction комітить і повертає результат")
    void commitsAndReturnsResult() {
        Long id = Tx.inTransaction(emf, em -> {
            Account account = new Account("UA100", CurrencyCode.EUR, BigDecimal.ONE);
            em.persist(account);
            return account.getId();
        });

        assertNotNull(id);
        assertEquals(1L, accountCount());
    }

    @Test
    @DisplayName("3 (резерв) · при помилці rollback і той самий виняток далі")
    void rollsBackAndRethrowsTheSameException() {
        IllegalStateException failure = new IllegalStateException("boom");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                Tx.runInTransaction(emf, em -> {
                    em.persist(new Account("UA101", CurrencyCode.EUR, BigDecimal.ONE));
                    em.flush(); // INSERT уже в базі, але транзакція не завершена
                    throw failure;
                }));

        assertSame(failure, thrown, "Помилку не можна ковтати або підміняти");
        assertEquals(0L, accountCount(), "INSERT після flush теж відкочується");
    }

    private long accountCount() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select count(a) from Account a", Long.class).getSingleResult();
        }
    }
}
