package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleStateException;

import java.math.BigDecimal;

/** Спільний код для обох сервісів переказу. */
final class TransferSupport {

    private TransferSupport() {
    }

    static void validate(Long fromId, Long toId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сума переказу має бути більшою за нуль");
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Не можна переказати кошти на той самий рахунок");
        }
    }

    static void move(EntityManager em, Account from, Account to, BigDecimal amount) {
        from.debit(amount);
        to.credit(amount);
        em.persist(Transaction.debit(from.getId(), amount, "Переказ на " + to.getNumber()));
        em.persist(Transaction.credit(to.getId(), amount, "Переказ з " + from.getNumber()));
    }

    /**
     * Конфлікт версій може прийти різними обгортками: OptimisticLockException з flush(),
     * RollbackException з commit() з OptimisticLockException всередині, або StaleStateException Hibernate.
     * Тому перевіряємо весь ланцюжок причин.
     */
    static boolean isOptimisticLockFailure(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof OptimisticLockException || t instanceof StaleStateException) {
                return true;
            }
        }
        return false;
    }
}
