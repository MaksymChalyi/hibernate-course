package ua.minibank;

import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

/**
 * Звіти Mini Bank.
 * Дані для перевірки — у DemoData (відкрий і подивись: 3 клієнти, 4 рахунки, 14 операцій у січні–лютому 2026).
 * Очікувані значення — у ReportRepositoryTest.
 *
 * Шаблон кожного методу однаковий:
 *     try (EntityManager em = emf.createEntityManager()) {
 *         return em.createQuery("""
 *                 ... JPQL ...
 *                 """, РезультатНийТип.class)
 *                 .setParameter("ім'я", значення)
 *                 .getResultList();
 *     }
 */
public class ReportRepository {

    private final EntityManagerFactory emf;

    public ReportRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ---------- Обов'язкова частина ----------

    /*
     * Завдання 1. Оборот по рахунку за місяць: скільки надійшло, скільки списано, скільки операцій.
     *
     * Крок 1. Межі місяця за КИЇВСЬКИМ часом (DemoData.KYIV):
     *     Instant from = month.atDay(1).atStartOfDay(DemoData.KYIV).toInstant();
     *     Instant to   = month.plusMonths(1).atDay(1).atStartOfDay(DemoData.KYIV).toInstant();
     *   Для січня 2026: from = 1 січня 00:00 Києва, to = 1 лютого 00:00 Києва.
     *   Права межа ВИКЛЮЧНА: у запиті буде "createdAt < :to" — так операція рівно о 00:00 1 лютого
     *   не потрапить в обидва місяці.
     *
     * Крок 2. JPQL з групуванням за типом. Результат — record TypeTotal (тип, сума, кількість):
     *     select new ua.minibank.TypeTotal(t.type, sum(t.amount), count(t))
     *     from Transaction t
     *     where t.account.number = :number
     *       and t.createdAt >= :from and t.createdAt < :to
     *     group by t.type
     *   Поверне до двох рядків: TypeTotal(CREDIT, 20000, 1) і TypeTotal(DEBIT, 6000, 3).
     *   Параметри: .setParameter("number", accountNumber).setParameter("from", from).setParameter("to", to)
     *
     * Крок 3. Зібрати MonthlyTurnover:
     *     BigDecimal credit = BigDecimal.ZERO, debit = BigDecimal.ZERO; long count = 0;
     *     for (TypeTotal total : totals) {
     *         if (total.type() == TransactionType.CREDIT) credit = total.total(); else debit = total.total();
     *         count += total.count();
     *     }
     *     return new MonthlyTurnover(accountNumber, month, credit, debit, count);
     *   Якщо операцій немає — список порожній, і повернуться нулі (тест «місяць без операцій»).
     *
     * Перевірка: тести «1 · …». Потім експеримент: заміни DemoData.KYIV на java.time.ZoneOffset.UTC — що стане з січнем?
     */
    public MonthlyTurnover monthlyTurnover(String accountNumber, YearMonth month) {
        // TODO 1 Оборот за місяць: межі за Києвом, JPQL з group by t.type → TypeTotal, зібрати MonthlyTurnover
        throw new UnsupportedOperationException("TODO 1: monthlyTurnover — кроки в коментарі над методом");
    }

    /*
     * Завдання 2. Сторінка операцій рахунку, нові зверху. page рахується з нуля.
     *
     *     select new ua.minibank.TransactionView(t.id, t.type, t.amount, t.description, t.createdAt)
     *     from Transaction t
     *     where t.account.number = :number
     *     order by t.createdAt desc, t.id desc
     *
     * Пагінація — два виклики перед getResultList():
     *     .setFirstResult(page * size)   — скільки рядків пропустити (SQL: OFFSET)
     *     .setMaxResults(size)           — скільки взяти (SQL: LIMIT)
     *
     * Чому в order by ще й t.id: якщо дві операції мають однаковий час, без другого ключа сортування
     * база може повертати їх у різному порядку — і рядок з'явиться на двох сторінках, а інший зникне.
     *
     * Перевірка: тести «2 · …».
     */
    public List<TransactionView> findTransactions(String accountNumber, int page, int size) {
        // TODO 2 Сторінка операцій: DTO TransactionView, order by createdAt desc, id desc, setFirstResult/setMaxResults
        throw new UnsupportedOperationException("TODO 2: findTransactions — кроки в коментарі над методом");
    }

    // ---------- Домашнє завдання ----------

    /*
     * Завдання 3 (ДЗ). Топ клієнтів за сумою СПИСАНЬ у заданій валюті за період [from; to).
     *
     *     select new ua.minibank.CustomerSpending(c.id, c.firstName, c.lastName, sum(t.amount))
     *     from Transaction t
     *     join t.account a
     *     join a.customer c
     *     where t.type = :debit
     *       and a.currency = :currency
     *       and t.createdAt >= :from and t.createdAt < :to
     *     group by c.id, c.firstName, c.lastName
     *     order by sum(t.amount) desc
     *
     * Параметри: debit = TransactionType.DEBIT, currency, from, to.
     * Кількість рядків: .setMaxResults(limit).
     * Чому валюта обов'язкова: 100 UAH + 100 USD — число без сенсу.
     */
    public List<CustomerSpending> topCustomersBySpending(CurrencyCode currency, Instant from, Instant to, int limit) {
        // TODO 3 (ДЗ) Топ клієнтів за сумою списань: JPQL з двома join, group by, order by sum desc, setMaxResults
        throw new UnsupportedOperationException("TODO 3: topCustomersBySpending — кроки в коментарі над методом");
    }

    /*
     * Завдання 4 (ДЗ). Той самий звіт на native SQL — імена ТАБЛИЦЬ і КОЛОНОК, а не класів і полів.
     *
     *     select c.id, c.first_name, c.last_name, sum(t.amount) as total
     *     from transactions t
     *     join accounts a on a.id = t.account_id
     *     join customers c on c.id = a.customer_id
     *     where t.type = 'DEBIT' and a.currency = :currency
     *       and t.created_at >= :from and t.created_at < :to
     *     group by c.id, c.first_name, c.last_name
     *     order by total desc
     *     limit :limit
     *
     * em.createNativeQuery(sql) БЕЗ класу результату → getResultList() поверне List<Object[]>:
     * кожен рядок — масив колонок. Перетвори вручну:
     *     new CustomerSpending(((Number) r[0]).longValue(), (String) r[1], (String) r[2], (BigDecimal) r[3])
     * Валюту передавай рядком: .setParameter("currency", currency.name())
     */
    public List<CustomerSpending> topCustomersBySpendingNative(CurrencyCode currency, Instant from, Instant to, int limit) {
        // TODO 4 (ДЗ) Той самий звіт через em.createNativeQuery, рядки Object[] → CustomerSpending
        throw new UnsupportedOperationException("TODO 4: topCustomersBySpendingNative — кроки в коментарі над методом");
    }

    // ---------- Резерв ----------

    /*
     * Завдання 5 (резерв). Пошук за фільтром через Criteria API: умова додається, лише якщо поле фільтра не null.
     *
     *     CriteriaBuilder cb = em.getCriteriaBuilder();
     *     CriteriaQuery<TransactionView> query = cb.createQuery(TransactionView.class);
     *     Root<Transaction> t = query.from(Transaction.class);           // «from Transaction t»
     *     Join<Transaction, Account> a = t.join("account");              // «join t.account a»
     *
     *     List<Predicate> where = new ArrayList<>();
     *     if (filter.accountNumber() != null) where.add(cb.equal(a.get("number"), filter.accountNumber()));
     *     if (filter.type() != null)          where.add(cb.equal(t.get("type"), filter.type()));
     *     if (filter.minAmount() != null)     where.add(cb.greaterThanOrEqualTo(t.get("amount"), filter.minAmount()));
     *     ... так само currency (a.get("currency")), maxAmount (lessThanOrEqualTo), from (>=), to (lessThan) ...
     *
     *     query.select(cb.construct(TransactionView.class,
     *                 t.get("id"), t.get("type"), t.get("amount"), t.get("description"), t.get("createdAt")))
     *          .where(where.toArray(Predicate[]::new))
     *          .orderBy(cb.desc(t.get("createdAt")), cb.desc(t.get("id")));
     *     return em.createQuery(query).getResultList();
     *
     * Імпорти: jakarta.persistence.criteria.* і java.util.ArrayList.
     */
    public List<TransactionView> search(TransactionFilter filter) {
        // TODO 5 (резерв) Динамічний фільтр через Criteria API (кроки над методом)
        throw new UnsupportedOperationException("TODO 5: search — кроки в коментарі над методом");
    }

    /*
     * Завдання 6 (резерв). Залишок після кожної операції — native SQL з window function:
     *
     *     select t.id, t.amount,
     *            sum(case when t.type = 'CREDIT' then t.amount else -t.amount end)
     *                over (order by t.created_at, t.id) as balance_after
     *     from transactions t
     *     join accounts a on a.id = t.account_id
     *     where a.number = :number
     *     order by t.created_at, t.id
     *
     * sum(...) over (order by ...) — «накопичена сума»: для кожного рядка сума всіх попередніх і його самого.
     * У JPQL такого немає — тому native. Рядки Object[] → new BalancePoint(id, amount, balanceAfter).
     */
    public List<BalancePoint> runningBalance(String accountNumber) {
        // TODO 6 (резерв) Накопичений залишок через native SQL з sum(...) over (...)
        throw new UnsupportedOperationException("TODO 6: runningBalance — кроки в коментарі над методом");
    }

    /*
     * Завдання 7 (резерв). Bulk update: списати комісію fee з УСІХ рахунків у валюті одним UPDATE.
     *
     *     update Account a set a.balance = a.balance - :fee
     *     where a.currency = :currency and a.balance >= :fee
     *
     * em.createQuery(jpql).setParameter(...).executeUpdate() — повертає кількість змінених рядків.
     * Потрібна транзакція: em.getTransaction().begin() ... commit().
     * Після цього запусти тест «7 (резерв) · …» і поясни, що він показує про Persistence Context.
     */
    public int chargeMonthlyFee(CurrencyCode currency, BigDecimal fee) {
        // TODO 7 (резерв) Один UPDATE на всі рахунки через executeUpdate у транзакції
        throw new UnsupportedOperationException("TODO 7: chargeMonthlyFee — кроки в коментарі над методом");
    }
}
