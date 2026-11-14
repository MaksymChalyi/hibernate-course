package ua.minibank;

import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;

public class TransferService {

    private final EntityManagerFactory emf;
    private final Runnable afterDebit;

    public TransferService(EntityManagerFactory emf) {
        this(emf, () -> {
        });
    }

    /** afterDebit — точка для тестів: тест кидає тут виняток, щоб перевірити rollback. */
    TransferService(EntityManagerFactory emf, Runnable afterDebit) {
        this.emf = emf;
        this.afterDebit = afterDebit;
    }

    /**
     * TODO 1. Переказ коштів в одній транзакції:
     *   0. Перевірити аргументи: amount > 0, fromId != toId → інакше IllegalArgumentException.
     *   1. Відкрити EntityManager, почати транзакцію.
     *   2. Знайти обидва рахунки. Якщо рахунку немає — AccountNotFoundException.
     *   3. from.debit(amount) і em.persist(Transaction.debit(...)).
     *   4. afterDebit.run() — не прибирай, на цьому тримається тест на rollback.
     *   5. to.credit(amount) і em.persist(Transaction.credit(...)).
     *   6. commit.
     *   7. При будь-якому RuntimeException: rollback (якщо транзакція активна) і кинути виняток далі.
     *
     * TODO 2 (домашнє завдання): якщо валюти рахунків різні — CurrencyMismatchException.
     */
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        throw new UnsupportedOperationException("TODO: transfer");
    }
}
