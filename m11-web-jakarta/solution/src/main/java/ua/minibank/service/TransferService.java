package ua.minibank.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import ua.minibank.domain.Account;
import ua.minibank.domain.AccountNotFoundException;
import ua.minibank.domain.CurrencyMismatchException;
import ua.minibank.domain.Transaction;
import ua.minibank.domain.TransactionType;

import java.math.BigDecimal;

/**
 * Переказ із модулів 4 і 9: одна транзакція, pessimistic lock у порядку id.
 * Servlet не знає ні про EntityManager, ні про транзакції — лише викликає метод сервісу.
 */
public class TransferService {

    private final EntityManagerFactory emf;

    public TransferService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        if (fromId == null || toId == null) {
            throw new IllegalArgumentException("Потрібні fromAccountId і toAccountId");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сума переказу має бути більшою за нуль");
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Не можна переказати кошти на той самий рахунок");
        }

        Tx.runInTransaction(emf, em -> {
            Long firstId = Math.min(fromId, toId);
            Account first = lock(em, firstId);
            Account second = lock(em, Math.max(fromId, toId));
            Account from = fromId.equals(firstId) ? first : second;
            Account to = fromId.equals(firstId) ? second : first;

            if (from.getCurrency() != to.getCurrency()) {
                throw new CurrencyMismatchException(from.getCurrency(), to.getCurrency());
            }
            from.debit(amount);
            to.credit(amount);
            em.persist(new Transaction(from, TransactionType.DEBIT, amount, "Переказ на " + to.getNumber()));
            em.persist(new Transaction(to, TransactionType.CREDIT, amount, "Переказ з " + from.getNumber()));
        });
    }

    private static Account lock(EntityManager em, Long id) {
        Account account = em.find(Account.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (account == null) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }
}
