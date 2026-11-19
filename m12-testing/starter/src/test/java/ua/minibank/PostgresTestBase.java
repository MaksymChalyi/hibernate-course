package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;

/**
 * Базовий клас інтеграційних тестів на справжньому PostgreSQL.
 * Усі тестові класи модуля наслідують його: class TransferServiceTest extends PostgresTestBase.
 *
 * Яку саме базу брати (Docker чи локальну схему m12_test), вирішує готовий клас TestDatabase.
 * Без Docker нічого робити не треба — буде локальний PostgreSQL.
 */
public abstract class PostgresTestBase {

    /** Готово: адреса, користувач, пароль і схема тестової бази. */
    protected static final TestDatabase DB = TestDatabase.start("m12_test");

    protected static final EntityManagerFactory EMF;

    /*
     * Завдання 1. Створити таблиці в тестовій базі тими самими міграціями, що й у застосунку.
     * Завдання 2. Створити EntityManagerFactory, що дивиться на тестову базу.
     *
     * Імпорти: jakarta.persistence.Persistence, org.flywaydb.core.Flyway, java.util.Map
     *
     * Код (заміни ним рядок EMF = null):
     *
     *     Flyway.configure()
     *             .dataSource(DB.url(), DB.user(), DB.password())
     *             .schemas(DB.schema())
     *             .load()
     *             .migrate();                                     // таблиці з src/main/resources/db/migration
     *
     *     EMF = Persistence.createEntityManagerFactory("minibank", Map.of(
     *             "jakarta.persistence.jdbc.url", DB.url(),
     *             "jakarta.persistence.jdbc.user", DB.user(),
     *             "jakarta.persistence.jdbc.password", DB.password(),
     *             "hibernate.default_schema", DB.schema()));
     *
     * Це те саме, що AppContextListener у модулі 11, лише з іншою адресою бази.
     *
     * Чому static-блок, а не @BeforeAll у кожному класі: static-поле базового класу ініціалізується
     * ОДИН раз на запуск усіх тестів. Фабрика і міграції — дорогі, повторювати їх для кожного класу немає сенсу.
     */
    static {
        // TODO 1 Flyway.migrate() для тестової бази (код у коментарі вище)
        // TODO 2 EMF = Persistence.createEntityManagerFactory(...) з адресою з DB
        EMF = null;
    }

    /*
     * Завдання 3. Кожен тест починається з порожніх таблиць.
     *
     *     Tx.runInTransaction(EMF, em ->
     *             em.createNativeQuery("truncate table transactions, accounts, customers").executeUpdate());
     *     statistics().clear();                                   // лічильник запитів — з нуля
     *
     * Імпорт: ua.minibank.service.Tx
     *
     * Чому TRUNCATE усіх трьох таблиць одним запитом: між ними є зовнішні ключі,
     * і PostgreSQL дозволяє очистити пов'язані таблиці лише разом.
     *
     * Питання: чому не можна покладатися на порядок, у якому JUnit запускає тести?
     */
    @BeforeEach
    void cleanDatabase() {
        // TODO 3 Очистити таблиці і статистику перед кожним тестом (код у коментарі вище)
    }

    /** Лічильники Hibernate: скільки запитів, вставок тощо. Увімкнено в persistence.xml (generate_statistics). */
    protected static Statistics statistics() {
        return EMF.unwrap(SessionFactory.class).getStatistics();
    }
}
