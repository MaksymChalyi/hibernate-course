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

    /**
     * TODO 2. Зараз рахунок читається без блокування — паралельні перекази втрачають оновлення.
     * Заблокуй рядок до кінця транзакції: em.find(Account.class, id, LockModeType.PESSIMISTIC_WRITE).
     * Подивись у лог, який SQL з'явився.
     *
     * TODO 3 (резерв). Запусти oppositeTransfersDoNotDeadlock. Чому виникає deadlock?
     * Виправ transfer: блокуй рахунки завжди в одному порядку (за зростанням id).
     */
    private static Account lock(EntityManager em, Long id) {
        Account account = em.find(Account.class, id);
        if (account == null) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }
}
