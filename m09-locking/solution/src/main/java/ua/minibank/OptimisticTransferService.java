package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Переказ з optimistic locking: без блокувань під час читання.
 * Конфлікт виявляється при commit (через @Version), і тоді вся операція повторюється з нуля.
 *
 * Добре, коли конфлікти рідкісні: переказ між рахунками різних клієнтів.
 * Погано для "гарячого" рахунку, з якого одночасно списують десятки потоків: багато повторів.
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
     * Домашнє завдання: retry.
     * Повторюємо лише конфлікт версій. InsufficientFundsException чи AccountNotFoundException
     * повторювати немає сенсу: результат буде той самий.
     * Кожна спроба — новий EntityManager: старий Persistence Context містить застарілі дані.
     */
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        TransferSupport.validate(fromId, toId, amount);
        for (int attempt = 1; ; attempt++) {
            try {
                transferOnce(fromId, toId, amount);
                return;
            } catch (RuntimeException e) {
                if (!TransferSupport.isOptimisticLockFailure(e) || attempt >= maxAttempts) {
                    throw e;
                }
                backoff(attempt);
            }
        }
    }

    private void transferOnce(Long fromId, Long toId, BigDecimal amount) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Account from = find(em, fromId);
                Account to = find(em, toId);
                TransferSupport.move(em, from, to, amount);
                tx.commit(); // тут UPDATE ... where id = ? and version = ?
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    /** Випадкова пауза, що росте з кожною спробою: потоки не зіштовхуються знову в ту саму мить. */
    private static void backoff(int attempt) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(1, 5L * attempt + 2));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Переказ перервано", e);
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
