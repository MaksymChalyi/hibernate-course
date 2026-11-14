package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.Map;

/**
 * Дві фабрики з різними налаштуваннями: без batching (як за замовчуванням) і з ним.
 * hbm2ddl = none: таблицю створює Main/тест один раз через schema().
 */
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
     * jdbc.batch_size — скільки INSERT/UPDATE відправляти одним запитом до бази.
     * order_inserts / order_updates — групувати однакові запити, щоб пачки не розривались,
     * коли в одній транзакції змішані різні entity.
     */
    public static EntityManagerFactory withBatching(int batchSize) {
        return Persistence.createEntityManagerFactory("minibank", Map.of(
                "hibernate.jdbc.batch_size", String.valueOf(batchSize),
                "hibernate.order_inserts", "true",
                "hibernate.order_updates", "true"));
    }

    public static Statistics statistics(EntityManagerFactory emf) {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }
}
