package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;

/**
 * Переказ з optimistic locking (потрібен @Version в Account).
 */
public class OptimisticTransferService {

    private final EntityManagerFactory emf;
    private final int maxAttempts;

    public OptimisticTransferService(EntityManagerFactory emf, int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts має бути не менше 1");
        }
        this.emf = emf;
        this.maxAttempts = maxAttempts;
    }

    /*
     * Завдання 4 (ДЗ). Повтор (retry) при конфлікті версій.
     *
     * З @Version паралельний переказ може впасти з конфліктом. Це нормально: треба просто спробувати ще раз
     * з актуальними даними. Але НЕ кожну помилку можна повторювати.
     *
     * Скелет:
     *     TransferSupport.validate(fromId, toId, amount);
     *     for (int attempt = 1; ; attempt++) {
     *         try {
     *             transferOnce(fromId, toId, amount);   // кожна спроба — НОВИЙ EntityManager (так уже зроблено в transferOnce)
     *             return;                               // успіх
     *         } catch (RuntimeException e) {
     *             if (!TransferSupport.isOptimisticLockFailure(e) || attempt >= maxAttempts) {
     *                 throw e;                          // не конфлікт версій або спроби скінчились
     *             }
     *             Thread.sleep(випадкова пауза 1..(5 * attempt) мс);   // ThreadLocalRandom.current().nextLong(...)
     *         }
     *     }
     *
     * Чому не повторювати InsufficientFundsException: грошей від повтору не додасться.
     * Навіщо випадкова пауза: без неї потоки знову зіткнуться в ту саму мить.
     * Thread.sleep кидає InterruptedException — обгорни: catch → Thread.currentThread().interrupt(); throw new IllegalStateException(e);
     *
     * Перевірка: тест «4 (ДЗ) · optimistic з retry».
     */
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        // TODO 4 (ДЗ) Повторювати transferOnce при конфлікті версій, не більше maxAttempts разів (скелет над методом)
        TransferSupport.validate(fromId, toId, amount);
        transferOnce(fromId, toId, amount);
    }

    private void transferOnce(Long fromId, Long toId, BigDecimal amount) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Account from = find(em, fromId);
                Account to = find(em, toId);
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

    private static Account find(EntityManager em, Long id) {
        Account account = em.find(Account.class, id);
        if (account == null) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }
}
