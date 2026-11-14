package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import ua.minibank.service.Tx;

import java.util.Map;

/**
 * Базовий клас інтеграційних тестів на справжньому PostgreSQL.
 *
 * Static-блок виконується один раз на весь запуск тестів, а не на кожен клас:
 * база (контейнер або локальна схема) готується один раз.
 *
 * Схему створює Flyway тими самими міграціями, що й у production.
 * Hibernate з validate перевіряє, що entity відповідають міграціям.
 */
public abstract class PostgresTestBase {

    protected static final TestDatabase DB = TestDatabase.start("m12_test");
    protected static final EntityManagerFactory EMF;

    static {
        Flyway.configure()
                .dataSource(DB.url(), DB.user(), DB.password())
                .schemas(DB.schema())
                .load()
                .migrate();

        EMF = Persistence.createEntityManagerFactory("minibank", Map.of(
                "jakarta.persistence.jdbc.url", DB.url(),
                "jakarta.persistence.jdbc.user", DB.user(),
                "jakarta.persistence.jdbc.password", DB.password(),
                "hibernate.default_schema", DB.schema()));
    }

    /**
     * Кожен тест починається з порожніх таблиць: тести не залежать від порядку запуску.
     * TRUNCATE швидший за DELETE і за перестворення схеми.
     */
    @BeforeEach
    void cleanDatabase() {
        Tx.runInTransaction(EMF, em ->
                em.createNativeQuery("truncate table transactions, accounts, customers").executeUpdate());
        statistics().clear();
    }

    protected static Statistics statistics() {
        return EMF.unwrap(SessionFactory.class).getStatistics();
    }
}
