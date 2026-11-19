package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Демонстрація: той самий наївний цикл з default_batch_fetch_size і без нього.
 * Код не змінюється, змінюється лише налаштування.
 */
@DisplayName("Модуль 6 · Демо: batch fetching")
class BatchFetchTest {

    private static final int CUSTOMERS = 20;

    @Test
    @DisplayName("Демо · наївний цикл без batch fetching — N+1")
    void naiveLoopIsNPlusOneWithoutBatchFetching() {
        long queries = runNaiveLoop(Map.of());

        System.out.println("Без batch fetching: " + queries + " запитів");
        assertTrue(queries > CUSTOMERS, "Очікуємо 1 + " + CUSTOMERS + " запитів");
    }

    @Test
    @DisplayName("Демо · з default_batch_fetch_size = 16 — пачками")
    void batchFetchingLoadsOwnersInChunks() {
        long queries = runNaiveLoop(Map.of("hibernate.default_batch_fetch_size", "16"));

        System.out.println("З batch fetching (16): " + queries + " запитів");
        // 1 запит на рахунки + ceil(20 / 16) = 2 пачки клієнтів
        assertTrue(queries <= 3, "Клієнти мають довантажуватися пачками, а не по одному");
    }

    private long runNaiveLoop(Map<String, String> extraSettings) {
        Map<String, Object> settings = new java.util.HashMap<>(extraSettings);
        try (EntityManagerFactory seedEmf = Persistence.createEntityManagerFactory("minibank")) {
            DataSeeder.seed(seedEmf, CUSTOMERS);
        }
        settings.put("hibernate.hbm2ddl.auto", "none"); // не перестворювати таблиці з даними

        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank", settings)) {
            Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
            stats.clear();
            try (EntityManager em = emf.createEntityManager()) {
                List<Account> accounts = em.createQuery("select a from Account a", Account.class).getResultList();
                for (Account account : accounts) {
                    account.getCustomer().getFullName();
                }
            }
            return stats.getPrepareStatementCount();
        }
    }
}
