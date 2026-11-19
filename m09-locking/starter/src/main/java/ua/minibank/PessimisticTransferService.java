package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;

public class PessimisticTransferService {

    private final EntityManagerFactory emf;

    public PessimisticTransferService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        TransferSupport.validate(fromId, toId, amount);

        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();

                Account from = lock(em, fromId);
                Account to = lock(em, toId);
                TransferSupport.move(em, from, to, amount);

                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    /*
     * Завдання 2. Заблокувати рядок рахунку до кінця транзакції.
     *
     * Зараз рахунок читається звичайним em.find — без блокування. Два паралельні перекази читають
     * той самий баланс і обидва записують свій результат — одне списання губиться (lost update).
     *
     * Заміни em.find(Account.class, id) на:
     *     em.find(Account.class, id, LockModeType.PESSIMISTIC_WRITE)
     * (import jakarta.persistence.LockModeType)
     *
     * Hibernate додасть до SELECT "for update" (або "for no key update" для PostgreSQL).
     * Інша транзакція, що хоче заблокувати той самий рядок, ЧЕКАТИМЕ, поки ми не зробимо commit.
     *
     * Перевірка: тест «2 · pessimistic: 100 паралельних переказів — баланс коректний».
     *
     * Завдання 3 (резерв). Запусти тест «3 (резерв) · зустрічні перекази без deadlock».
     * Перекази A→B і B→A одночасно: перший заблокував A і чекає B, другий заблокував B і чекає A.
     * Виправ transfer(): блокуй рахунки ЗАВЖДИ в порядку зростання id:
     *     Long firstId = Math.min(fromId, toId);
     *     Long secondId = Math.max(fromId, toId);
     *     Account first = lock(em, firstId);
     *     Account second = lock(em, secondId);
     *     Account from = fromId.equals(firstId) ? first : second;
     *     Account to   = fromId.equals(firstId) ? second : first;
     */
    // TODO 2 Читати рахунок з LockModeType.PESSIMISTIC_WRITE
    // TODO 3 (резерв) У transfer() блокувати рахунки в порядку зростання id (кроки над методом lock)
    private static Account lock(EntityManager em, Long id) {
        Account account = em.find(Account.class, id);
        if (account == null) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }
}
