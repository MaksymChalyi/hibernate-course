package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.Map;

public final class Emfs {

    private Emfs() {
    }

    /** Створює таблиці (hbm2ddl = create) і одразу закриває фабрику. */
    public static void createSchema() {
        Persistence.createEntityManagerFactory("minibank", Map.of("hibernate.hbm2ddl.auto", "create")).close();
    }

    public static EntityManagerFactory plain() {
        return Persistence.createEntityManagerFactory("minibank");
    }

    /**
     * TODO 2. Фабрика з увімкненим JDBC batching. Передай у createEntityManagerFactory другий аргумент —
     * Map з властивостями:
     *   hibernate.jdbc.batch_size = batchSize
     *   hibernate.order_inserts   = true
     *   hibernate.order_updates   = true
     * Питання: навіщо order_inserts, якщо в нас лише одна entity?
     */
    public static EntityManagerFactory withBatching(int batchSize) {
        return Persistence.createEntityManagerFactory("minibank");
    }

    public static Statistics statistics(EntityManagerFactory emf) {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }
}
