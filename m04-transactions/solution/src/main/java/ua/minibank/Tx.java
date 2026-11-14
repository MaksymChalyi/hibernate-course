package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Резерв: шаблон транзакції в одному місці замість повторюваного try/catch/rollback.
 * Саме це пізніше робитиме @Transactional у Spring.
 */
public final class Tx {

    private Tx() {
    }

    public static <T> T inTransaction(EntityManagerFactory emf, Function<EntityManager, T> work) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                T result = work.apply(em);
                tx.commit();
                return result;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    public static void runInTransaction(EntityManagerFactory emf, Consumer<EntityManager> work) {
        inTransaction(emf, em -> {
            work.accept(em);
            return null;
        });
    }
}
