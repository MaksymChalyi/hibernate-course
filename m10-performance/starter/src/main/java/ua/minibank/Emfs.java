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

    /*
     * Завдання 2. Фабрика з увімкненим JDBC batching.
     *
     * Batching — Hibernate накопичує однакові INSERT і відправляє їх пачкою (по batchSize штук) одним зверненням
     * до бази, а не по одному. Вмикається трьома властивостями. Передаємо їх другим аргументом:
     *
     *     return Persistence.createEntityManagerFactory("minibank", Map.of(
     *             "hibernate.jdbc.batch_size", String.valueOf(batchSize),
     *             "hibernate.order_inserts", "true",
     *             "hibernate.order_updates", "true"));
     *
     * order_inserts — групувати INSERT за таблицями. Якщо в одній транзакції чергуються різні entity
     * (операція, рахунок, операція...), без нього пачка розривається на кожній зміні таблиці.
     * У нас одна entity, але налаштування — стандартна практика.
     */
    public static EntityManagerFactory withBatching(int batchSize) {
        // TODO 2 Передати в createEntityManagerFactory Map з hibernate.jdbc.batch_size, order_inserts, order_updates
        return Persistence.createEntityManagerFactory("minibank");
    }

    public static Statistics statistics(EntityManagerFactory emf) {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }
}
