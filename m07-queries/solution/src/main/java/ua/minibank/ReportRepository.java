package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReportRepository {

    private final EntityManagerFactory emf;

    public ReportRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ---------- Обов'язкова частина ----------

    /**
     * Оборот по рахунку за місяць за київським часом.
     *
     * Межі місяця рахуються в Java: [перше число 00:00 Києва; перше число наступного місяця 00:00 Києва).
     * Права межа виключна (<), щоб операція рівно о півночі не потрапила в обидва місяці.
     * Групування за типом дає максимум два рядки; підсумок збираємо в Java.
     */
    public MonthlyTurnover monthlyTurnover(String accountNumber, YearMonth month) {
        Instant from = month.atDay(1).atStartOfDay(DemoData.KYIV).toInstant();
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay(DemoData.KYIV).toInstant();

        try (EntityManager em = emf.createEntityManager()) {
            List<TypeTotal> totals = em.createQuery("""
                            select new ua.minibank.TypeTotal(t.type, sum(t.amount), count(t))
                            from Transaction t
                            where t.account.number = :number
                              and t.createdAt >= :from and t.createdAt < :to
                            group by t.type
                            """, TypeTotal.class)
                    .setParameter("number", accountNumber)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getResultList();

            BigDecimal credit = BigDecimal.ZERO;
            BigDecimal debit = BigDecimal.ZERO;
            long count = 0;
            for (TypeTotal total : totals) {
                if (total.type() == TransactionType.CREDIT) {
                    credit = total.total();
                } else {
                    debit = total.total();
                }
                count += total.count();
            }
            return new MonthlyTurnover(accountNumber, month, credit, debit, count);
        }
    }

    /**
     * Сторінка операцій рахунку, нові зверху. page рахується з нуля.
     * Сортування обов'язково однозначне (createdAt, потім id), інакше сторінки можуть перекриватися.
     */
    public List<TransactionView> findTransactions(String accountNumber, int page, int size) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            select new ua.minibank.TransactionView(t.id, t.type, t.amount, t.description, t.createdAt)
                            from Transaction t
                            where t.account.number = :number
                            order by t.createdAt desc, t.id desc
                            """, TransactionView.class)
                    .setParameter("number", accountNumber)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();
        }
    }

    // ---------- Домашнє завдання ----------

    /**
     * Топ клієнтів за сумою списань у валюті за період.
     * Валюта обов'язкова: сума 100 UAH + 100 USD не має сенсу.
     */
    public List<CustomerSpending> topCustomersBySpending(CurrencyCode currency, Instant from, Instant to, int limit) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            select new ua.minibank.CustomerSpending(c.id, c.firstName, c.lastName, sum(t.amount))
                            from Transaction t
                            join t.account a
                            join a.customer c
                            where t.type = :debit
                              and a.currency = :currency
                              and t.createdAt >= :from and t.createdAt < :to
                            group by c.id, c.firstName, c.lastName
                            order by sum(t.amount) desc
                            """, CustomerSpending.class)
                    .setParameter("debit", TransactionType.DEBIT)
                    .setParameter("currency", currency)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    /** Той самий звіт на native SQL: імена таблиць і колонок, а не класів і полів. */
    public List<CustomerSpending> topCustomersBySpendingNative(CurrencyCode currency, Instant from, Instant to, int limit) {
        try (EntityManager em = emf.createEntityManager()) {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery("""
                            select c.id, c.first_name, c.last_name, sum(t.amount) as total
                            from transactions t
                            join accounts a on a.id = t.account_id
                            join customers c on c.id = a.customer_id
                            where t.type = 'DEBIT'
                              and a.currency = :currency
                              and t.created_at >= :from and t.created_at < :to
                            group by c.id, c.first_name, c.last_name
                            order by total desc
                            limit :limit
                            """)
                    .setParameter("currency", currency.name())
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .setParameter("limit", limit)
                    .getResultList();
            return rows.stream()
                    .map(r -> new CustomerSpending(((Number) r[0]).longValue(), (String) r[1], (String) r[2], (BigDecimal) r[3]))
                    .toList();
        }
    }

    // ---------- Резерв ----------

    /** Динамічний фільтр через Criteria API: умови додаються лише для заповнених полів. */
    public List<TransactionView> search(TransactionFilter filter) {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<TransactionView> query = cb.createQuery(TransactionView.class);
            Root<Transaction> t = query.from(Transaction.class);
            Join<Transaction, Account> a = t.join("account");

            List<Predicate> where = new ArrayList<>();
            if (filter.accountNumber() != null) {
                where.add(cb.equal(a.get("number"), filter.accountNumber()));
            }
            if (filter.currency() != null) {
                where.add(cb.equal(a.get("currency"), filter.currency()));
            }
            if (filter.type() != null) {
                where.add(cb.equal(t.get("type"), filter.type()));
            }
            if (filter.minAmount() != null) {
                where.add(cb.greaterThanOrEqualTo(t.get("amount"), filter.minAmount()));
            }
            if (filter.maxAmount() != null) {
                where.add(cb.lessThanOrEqualTo(t.get("amount"), filter.maxAmount()));
            }
            if (filter.from() != null) {
                where.add(cb.greaterThanOrEqualTo(t.get("createdAt"), filter.from()));
            }
            if (filter.to() != null) {
                where.add(cb.lessThan(t.get("createdAt"), filter.to()));
            }

            query.select(cb.construct(TransactionView.class,
                            t.get("id"), t.get("type"), t.get("amount"), t.get("description"), t.get("createdAt")))
                    .where(where.toArray(Predicate[]::new))
                    .orderBy(cb.desc(t.get("createdAt")), cb.desc(t.get("id")));

            return em.createQuery(query).getResultList();
        }
    }

    /**
     * Залишок після кожної операції. Window function SUM(...) OVER (...) є в PostgreSQL,
     * але її немає в JPQL — це випадок для native SQL.
     */
    public List<BalancePoint> runningBalance(String accountNumber) {
        try (EntityManager em = emf.createEntityManager()) {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery("""
                            select t.id,
                                   t.amount,
                                   sum(case when t.type = 'CREDIT' then t.amount else -t.amount end)
                                       over (order by t.created_at, t.id) as balance_after
                            from transactions t
                            join accounts a on a.id = t.account_id
                            where a.number = :number
                            order by t.created_at, t.id
                            """)
                    .setParameter("number", accountNumber)
                    .getResultList();
            return rows.stream()
                    .map(r -> new BalancePoint(((Number) r[0]).longValue(), (BigDecimal) r[1], (BigDecimal) r[2]))
                    .toList();
        }
    }

    /**
     * Bulk update: один UPDATE на всі рахунки замість завантаження кожного.
     * Увага: bulk-операція йде повз Persistence Context. Об'єкти Account, уже завантажені
     * в будь-якому відкритому EntityManager, лишаються зі старим балансом.
     */
    public int chargeMonthlyFee(CurrencyCode currency, BigDecimal fee) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            int updated = em.createQuery("""
                            update Account a
                            set a.balance = a.balance - :fee
                            where a.currency = :currency and a.balance >= :fee
                            """)
                    .setParameter("fee", fee)
                    .setParameter("currency", currency)
                    .executeUpdate();
            em.getTransaction().commit();
            return updated;
        }
    }
}
