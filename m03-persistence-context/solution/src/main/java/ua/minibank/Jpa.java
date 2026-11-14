package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Допоміжний клас курсу: одна EntityManagerFactory на застосунок і короткий запис транзакцій.
 * EntityManagerFactory дорога у створенні (читає mapping, підключається до бази),
 * тому вона одна. EntityManager дешевий і створюється на кожну операцію.
 */
public final class Jpa {

    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("minibank");

    private Jpa() {
    }

    public static EntityManagerFactory emf() {
        return EMF;
    }

    /** Лічильники Hibernate. Увімкнені через hibernate.generate_statistics у persistence.xml. */
    public static Statistics statistics() {
        return EMF.unwrap(SessionFactory.class).getStatistics();
    }

    public static <T> T inTransaction(Function<EntityManager, T> work) {
        try (EntityManager em = EMF.createEntityManager()) {
            em.getTransaction().begin();
            try {
                T result = work.apply(em);
                em.getTransaction().commit();
                return result;
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    public static void runInTransaction(Consumer<EntityManager> work) {
        inTransaction(em -> {
            work.accept(em);
            return null;
        });
    }
}
