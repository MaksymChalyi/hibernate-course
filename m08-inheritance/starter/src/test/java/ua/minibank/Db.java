package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Допоміжні методи для тестів наслідування. */
final class Db {

    private Db() {
    }

    static void run(EntityManagerFactory emf, Consumer<EntityManager> work) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            work.accept(em);
            em.getTransaction().commit();
        }
    }

    static <T> T get(EntityManagerFactory emf, Function<EntityManager, T> work) {
        try (EntityManager em = emf.createEntityManager()) {
            return work.apply(em);
        }
    }

    /** Імена таблиць у схемі, за алфавітом. */
    static List<String> tables(EntityManagerFactory emf, String schema) {
        List<?> rows = get(emf, em -> em.createNativeQuery("""
                        select cast(table_name as text) from information_schema.tables
                        where table_schema = :schema order by table_name
                        """)
                .setParameter("schema", schema)
                .getResultList());
        return rows.stream().map(Object::toString).toList();
    }

    static long count(EntityManagerFactory emf, String table) {
        return get(emf, em -> ((Number) em.createNativeQuery("select count(*) from " + table).getSingleResult()).longValue());
    }
}
