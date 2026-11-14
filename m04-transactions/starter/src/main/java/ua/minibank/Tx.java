package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Резерв. TODO 3: винести шаблон транзакції в одне місце.
 *
 * inTransaction має:
 *   - відкрити EntityManager і закрити його в будь-якому випадку;
 *   - begin → work.apply(em) → commit → повернути результат;
 *   - при RuntimeException зробити rollback і кинути ТОЙ САМИЙ виняток далі.
 * runInTransaction — те саме для коду без результату (можна через inTransaction).
 *
 * Перевірка: TxTest.
 */
public final class Tx {

    private Tx() {
    }

    public static <T> T inTransaction(EntityManagerFactory emf, Function<EntityManager, T> work) {
        throw new UnsupportedOperationException("TODO: inTransaction");
    }

    public static void runInTransaction(EntityManagerFactory emf, Consumer<EntityManager> work) {
        throw new UnsupportedOperationException("TODO: runInTransaction");
    }
}
