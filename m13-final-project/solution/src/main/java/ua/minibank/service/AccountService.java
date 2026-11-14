package ua.minibank.service;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import ua.minibank.api.AccountDto;
import ua.minibank.api.PageDto;
import ua.minibank.api.TransactionDto;
import ua.minibank.api.TransactionFilter;
import ua.minibank.domain.Account;
import ua.minibank.domain.AccountNotFoundException;
import ua.minibank.domain.Transaction;

import java.util.ArrayList;
import java.util.List;

public class AccountService {

    public static final int MAX_PAGE_SIZE = 100;

    private final EntityManagerFactory emf;

    public AccountService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /** Один запит: рахунок разом із власником. */
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

    /**
     * Історія з фільтрами і пагінацією. Три запити незалежно від кількості операцій:
     * перевірка рахунку, сторінка (DTO-проєкція, без entity) і загальна кількість.
     * Індекс (account_id, created_at desc) з V1 покриває фільтр і сортування.
     */
    public PageDto<TransactionDto> history(Long accountId, TransactionFilter filter, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page >= 0, 1 <= size <= " + MAX_PAGE_SIZE);
        }
        return Tx.read(emf, em -> {
            if (em.find(Account.class, accountId) == null) {
                throw new AccountNotFoundException(accountId);
            }
            CriteriaBuilder cb = em.getCriteriaBuilder();

            CriteriaQuery<TransactionDto> query = cb.createQuery(TransactionDto.class);
            Root<Transaction> t = query.from(Transaction.class);
            query.select(cb.construct(TransactionDto.class,
                            t.get("id"), t.get("type"), t.get("amount"), t.get("description"), t.get("createdAt")))
                    .where(conditions(cb, t, accountId, filter))
                    .orderBy(cb.desc(t.get("createdAt")), cb.desc(t.get("id")));
            List<TransactionDto> items = em.createQuery(query)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();

            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<Transaction> c = countQuery.from(Transaction.class);
            countQuery.select(cb.count(c)).where(conditions(cb, c, accountId, filter));
            long total = em.createQuery(countQuery).getSingleResult();

            return new PageDto<>(items, page, size, total);
        });
    }

    public PageDto<TransactionDto> history(Long accountId, int page, int size) {
        return history(accountId, TransactionFilter.none(), page, size);
    }

    private static Predicate[] conditions(CriteriaBuilder cb, Root<Transaction> t, Long accountId, TransactionFilter f) {
        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(t.get("account").get("id"), accountId));
        if (f.type() != null) {
            where.add(cb.equal(t.get("type"), f.type()));
        }
        if (f.from() != null) {
            where.add(cb.greaterThanOrEqualTo(t.get("createdAt"), f.from()));
        }
        if (f.to() != null) {
            where.add(cb.lessThan(t.get("createdAt"), f.to()));
        }
        if (f.minAmount() != null) {
            where.add(cb.greaterThanOrEqualTo(t.get("amount"), f.minAmount()));
        }
        if (f.maxAmount() != null) {
            where.add(cb.lessThanOrEqualTo(t.get("amount"), f.maxAmount()));
        }
        return where.toArray(Predicate[]::new);
    }
}
