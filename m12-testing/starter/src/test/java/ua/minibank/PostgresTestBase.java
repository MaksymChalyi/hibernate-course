package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;

/**
 * Базовий клас інтеграційних тестів на справжньому PostgreSQL.
 * Яку саме базу брати (Docker чи локальну), вирішує готовий клас TestDatabase.
 */
public abstract class PostgresTestBase {

    /** Готово: контейнер, якщо є Docker, інакше локальна схема m12_test. */
    protected static final TestDatabase DB = TestDatabase.start("m12_test");

    protected static final EntityManagerFactory EMF;

    static {
        /*
         * TODO 1. Міграції Flyway:
         *   Flyway.configure().dataSource(DB.url(), DB.user(), DB.password()).schemas(DB.schema()).load().migrate();
         *
         * TODO 2. EntityManagerFactory: Persistence.createEntityManagerFactory("minibank", Map.of(...)).
         *   Перевизнач jakarta.persistence.jdbc.url / user / password і hibernate.default_schema значеннями з DB.
         *
         * Питання: чому static-блок, а не @BeforeAll у кожному тестовому класі?
         */
        EMF = null;
    }

    /**
     * TODO 3. Перед кожним тестом — порожні таблиці: truncate table transactions, accounts, customers.
     * Потім statistics().clear().
     * Питання: чому не можна покладатися на порядок запуску тестів?
     */
    @BeforeEach
    void cleanDatabase() {
    }

    protected static Statistics statistics() {
        return EMF.unwrap(SessionFactory.class).getStatistics();
    }
}
