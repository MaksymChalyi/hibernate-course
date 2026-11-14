package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;

/**
 * Переказ між рахунками: одна бізнес-операція = одна транзакція бази даних.
 * Або виконуються всі чотири зміни (два баланси + два записи Transaction), або жодна.
 */
public class TransferService {

    private final EntityManagerFactory emf;
    private final Runnable afterDebit;

    public TransferService(EntityManagerFactory emf) {
        this(emf, () -> {
        });
    }

    /**
     * afterDebit — точка для тестів: виконується після списання, до зарахування.
     * Тест кидає тут виняток, щоб перевірити, що списання відкотиться.
     */
    TransferService(EntityManagerFactory emf, Runnable afterDebit) {
        this.emf = emf;
        this.afterDebit = afterDebit;
    }

    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сума переказу має бути більшою за нуль");
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Не можна переказати кошти на той самий рахунок");
        }

        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();

                Account from = findAccount(em, fromId);
                Account to = findAccount(em, toId);
                if (from.getCurrency() != to.getCurrency()) {
                    throw new CurrencyMismatchException(from.getCurrency(), to.getCurrency());
                }

                from.debit(amount);
                em.persist(Transaction.debit(from.getId(), amount, "Переказ на " + to.getNumber()));

                afterDebit.run();

                to.credit(amount);
                em.persist(Transaction.credit(to.getId(), amount, "Переказ з " + from.getNumber()));

                tx.commit();
            } catch (RuntimeException e) {
                // Будь-яка помилка до commit — відкат усього, що зроблено в цій транзакції
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    private Account findAccount(EntityManager em, Long id) {
        Account account = em.find(Account.class, id);
        if (account == null) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }
}
