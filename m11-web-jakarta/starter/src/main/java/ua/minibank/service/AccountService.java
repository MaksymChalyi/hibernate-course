package ua.minibank.service;

import jakarta.persistence.EntityManagerFactory;
import ua.minibank.api.AccountDto;
import ua.minibank.api.PageDto;
import ua.minibank.api.TransactionDto;
import ua.minibank.domain.Account;
import ua.minibank.domain.AccountNotFoundException;

import java.util.List;

/**
 * Сервіс повертає DTO, а не entity. EntityManager відкривається і закривається всередині методу:
 * за межами сервісу lazy-полів уже немає, і LazyInitializationException виникнути не може.
 */
public class AccountService {

    public static final int MAX_PAGE_SIZE = 100;

    private final EntityManagerFactory emf;

    public AccountService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public AccountDto getAccount(Long accountId) {
        return Tx.read(emf, em -> {
            List<Account> found = em.createQuery(
                            "select a from Account a join fetch a.customer where a.id = :id", Account.class)
                    .setParameter("id", accountId)
                    .getResultList();
            if (found.isEmpty()) {
                throw new AccountNotFoundException(accountId);
            }
            return AccountDto.from(found.get(0));
        });
    }

    /** Резерв: історія операцій сторінками. Розмір сторінки обмежений, щоб не віддати мільйон рядків. */
    public PageDto<TransactionDto> history(Long accountId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page >= 0, 1 <= size <= " + MAX_PAGE_SIZE);
        }
        return Tx.read(emf, em -> {
            if (em.find(Account.class, accountId) == null) {
                throw new AccountNotFoundException(accountId);
            }
            List<TransactionDto> items = em.createQuery("""
                            select new ua.minibank.api.TransactionDto(t.id, t.type, t.amount, t.description, t.createdAt)
                            from Transaction t
                            where t.account.id = :id
                            order by t.createdAt desc, t.id desc
                            """, TransactionDto.class)
                    .setParameter("id", accountId)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();
            Long total = em.createQuery("select count(t) from Transaction t where t.account.id = :id", Long.class)
                    .setParameter("id", accountId)
                    .getSingleResult();
            return new PageDto<>(items, page, size, total);
        });
    }
}
