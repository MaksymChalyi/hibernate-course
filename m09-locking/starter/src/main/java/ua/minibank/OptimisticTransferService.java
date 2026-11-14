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

    /**
     * TODO 4 (домашнє завдання). Retry.
     * Зараз при конфлікті версій переказ просто падає. Повторюй transferOnce, поки:
     *   - не вдалося, і помилка — конфлікт версій (TransferSupport.isOptimisticLockFailure);
     *   - кількість спроб < maxAttempts.
     * Інші помилки (немає коштів, немає рахунку) не повторюй — кидай одразу.
     * Між спробами зроби коротку випадкову паузу (Thread.sleep). Навіщо вона?
     */
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
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
