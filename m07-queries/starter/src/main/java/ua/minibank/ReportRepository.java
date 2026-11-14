package ua.minibank;

import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

/**
 * Звіти Mini Bank. Дані для перевірки — у DemoData, очікувані значення — у ReportRepositoryTest.
 */
public class ReportRepository {

    private final EntityManagerFactory emf;

    public ReportRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // ---------- Обов'язкова частина ----------

    /**
     * TODO 1. Оборот по рахунку за місяць (за київським часом, DemoData.KYIV).
     *   1. Порахуй межі: from = перше число місяця 00:00 Києва, to = перше число наступного місяця.
     *      Права межа виключна: t.createdAt < :to.
     *   2. JPQL з DTO-проєкцією, групування за типом операції:
     *      select new ua.minibank.TypeTotal(t.type, sum(t.amount), count(t)) ... group by t.type
     *   3. З двох рядків (CREDIT, DEBIT) збери MonthlyTurnover. Якщо операцій немає — нулі.
     * Питання: що зміниться, якщо рахувати межі місяця в UTC? Подивись на операцію "Таксі".
     */
    public MonthlyTurnover monthlyTurnover(String accountNumber, YearMonth month) {
        throw new UnsupportedOperationException("TODO: monthlyTurnover");
    }

    /**
     * TODO 2. Сторінка операцій рахунку (page з нуля), нові зверху.
     * DTO-проєкція в TransactionView, setFirstResult / setMaxResults.
     * Питання: чому сортування лише за createdAt недостатнє?
     */
    public List<TransactionView> findTransactions(String accountNumber, int page, int size) {
        throw new UnsupportedOperationException("TODO: findTransactions");
    }

    // ---------- Домашнє завдання ----------

    /**
     * TODO 3. Топ клієнтів за сумою списань (DEBIT) у заданій валюті за період [from; to).
     * JPQL: Transaction → account → customer, group by, order by sum desc, setMaxResults(limit).
     */
    public List<CustomerSpending> topCustomersBySpending(CurrencyCode currency, Instant from, Instant to, int limit) {
        throw new UnsupportedOperationException("TODO: topCustomersBySpending");
    }

    /**
     * TODO 4. Той самий звіт на native SQL (em.createNativeQuery).
     * Результат — List<Object[]>; перетвори рядки в CustomerSpending вручну.
     */
    public List<CustomerSpending> topCustomersBySpendingNative(CurrencyCode currency, Instant from, Instant to, int limit) {
        throw new UnsupportedOperationException("TODO: topCustomersBySpendingNative");
    }

    // ---------- Резерв ----------

    /**
     * TODO 5. Пошук за фільтром через Criteria API: умова додається лише для поля, яке не null.
     * Результат — TransactionView через cb.construct(...), сортування як у findTransactions.
     */
    public List<TransactionView> search(TransactionFilter filter) {
        throw new UnsupportedOperationException("TODO: search");
    }

    /**
     * TODO 6. Залишок після кожної операції рахунку (native SQL з window function):
     *   sum(case when type = 'CREDIT' then amount else -amount end) over (order by created_at, id)
     */
    public List<BalancePoint> runningBalance(String accountNumber) {
        throw new UnsupportedOperationException("TODO: runningBalance");
    }

    /**
     * TODO 7. Bulk update: списати fee з усіх рахунків у валюті, де balance >= fee. Повернути кількість рядків.
     * Після виконання запусти тест bulkUpdateBypassesPersistenceContext і поясни, що він показує.
     */
    public int chargeMonthlyFee(CurrencyCode currency, BigDecimal fee) {
        throw new UnsupportedOperationException("TODO: chargeMonthlyFee");
    }
}
