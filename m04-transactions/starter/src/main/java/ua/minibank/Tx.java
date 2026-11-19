package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

/*
 * Завдання 3 (резерв). Шаблон транзакції в одному місці.
 *
 * Шаблон try / begin / commit / catch / rollback ми вже писали кілька разів. Винесемо його в helper,
 * щоб у сервісах залишилась лише бізнес-логіка:
 *
 *     Long id = Tx.inTransaction(emf, em -> {
 *         Account account = new Account(...);
 *         em.persist(account);
 *         return account.getId();
 *     });
 *
 * Function<EntityManager, T> work — «шматок коду», який отримує EntityManager і повертає результат типу T.
 * Ми викликаємо його як work.apply(em).
 *
 * Кроки для inTransaction:
 *   1. try (EntityManager em = emf.createEntityManager()) {
 *   2.     EntityTransaction tx = em.getTransaction();
 *   3.     try {
 *              tx.begin();
 *              T result = work.apply(em);     — виконати переданий код
 *              tx.commit();
 *              return result;
 *          } catch (RuntimeException e) {
 *              if (tx.isActive()) { tx.rollback(); }
 *              throw e;                        — ТОЙ САМИЙ виняток, не загортати і не ковтати
 *          }
 *      }
 *
 * runInTransaction — те саме для коду без результату (Consumer). Найпростіше — через inTransaction:
 *     inTransaction(emf, em -> { work.accept(em); return null; });
 *
 * Саме це пізніше робитиме анотація @Transactional у Spring.
 * Перевірка: TxTest.
 */
public final class Tx {

    private Tx() {
    }

    public static <T> T inTransaction(EntityManagerFactory emf, Function<EntityManager, T> work) {
        // TODO 3 (резерв) Відкрити EntityManager, begin → work.apply(em) → commit; при помилці rollback і той самий виняток
        throw new UnsupportedOperationException("TODO 3: inTransaction — кроки в коментарі над класом");
    }

    public static void runInTransaction(EntityManagerFactory emf, Consumer<EntityManager> work) {
        // TODO 3 (резерв) Викликати inTransaction з work.accept(em) і return null
        throw new UnsupportedOperationException("TODO 3: runInTransaction — кроки в коментарі над класом");
    }
}
