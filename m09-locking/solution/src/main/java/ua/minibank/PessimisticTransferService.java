package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;

/**
 * Переказ з pessimistic locking: рядки рахунків блокуються при читанні
 * (SELECT ... FOR UPDATE) до кінця транзакції. Інші транзакції чекають.
 *
 * Добре для "гарячих" рахунків і коротких транзакцій: жодних повторів.
 * Погано, якщо транзакція довга: усі чекають, а з'єднання з базою зайняті.
 */
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

                // Резерв: блокуємо завжди в порядку зростання id.
                // Інакше переказ A→B блокує A і чекає B, а паралельний B→A блокує B і чекає A — deadlock.
                Long firstId = Math.min(fromId, toId);
                Long secondId = Math.max(fromId, toId);
                Account first = lock(em, firstId);
                Account second = lock(em, secondId);

                Account from = fromId.equals(firstId) ? first : second;
                Account to = fromId.equals(firstId) ? second : first;
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

    private static Account lock(EntityManager em, Long id) {
        Account account = em.find(Account.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (account == null) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }
}
