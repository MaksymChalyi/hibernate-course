package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Модуль 7 · Запити")
class ReportRepositoryTest {

    private static final Instant Q1_FROM = DemoData.kyiv("2026-01-01T00:00");
    private static final Instant Q1_TO = DemoData.kyiv("2026-04-01T00:00");

    private static EntityManagerFactory emf;
    private final ReportRepository repository = new ReportRepository(emf);

    @BeforeAll
    static void start() {
        emf = Persistence.createEntityManagerFactory("minibank");
    }

    @AfterAll
    static void stop() {
        emf.close();
    }

    @BeforeEach
    void seed() {
        DemoData.seed(emf);
    }

    // ---------- Обов'язкова частина ----------

    @Test
    @DisplayName("1 · оборот за січень (межі за Києвом)")
    void januaryTurnoverUsesKyivTime() {
        MonthlyTurnover january = repository.monthlyTurnover("UA01", YearMonth.of(2026, 1));

        assertMoney("20000", january.totalCredit());
        // 1500 + 3500 + 1000. Таксі 01.02 00:30 за Києвом — це лютий, хоча за UTC ще січень
        assertMoney("6000", january.totalDebit());
        assertEquals(4, january.transactionCount());
    }

    @Test
    @DisplayName("1 · оборот за лютий")
    void februaryTurnover() {
        MonthlyTurnover february = repository.monthlyTurnover("UA01", YearMonth.of(2026, 2));

        assertMoney("20000", february.totalCredit());
        assertMoney("3200", february.totalDebit());
        assertEquals(3, february.transactionCount());
    }

    @Test
    @DisplayName("1 · місяць без операцій — нулі")
    void emptyMonthGivesZeros() {
        MonthlyTurnover march = repository.monthlyTurnover("UA01", YearMonth.of(2026, 3));

        assertMoney("0", march.totalCredit());
        assertMoney("0", march.totalDebit());
        assertEquals(0, march.transactionCount());
    }

    @Test
    @DisplayName("2 · перша сторінка — найновіші операції")
    void firstPageHasNewestTransactions() {
        List<TransactionView> page = repository.findTransactions("UA01", 0, 3);

        assertEquals(List.of("Подарунок", "Зарплата", "Таксі"),
                page.stream().map(TransactionView::description).toList());
    }

    @Test
    @DisplayName("2 · остання сторінка — решта")
    void lastPageHasTheRest() {
        List<TransactionView> page = repository.findTransactions("UA01", 2, 3);

        assertEquals(1, page.size());
        assertEquals(DemoData.kyiv("2026-01-05T10:00"), page.get(0).createdAt());
    }

    // ---------- Домашнє завдання ----------

    @Test
    @DisplayName("3 (ДЗ) · топ клієнтів у UAH за квартал")
    void topCustomersInUahForQuarter() {
        List<CustomerSpending> top = repository.topCustomersBySpending(CurrencyCode.UAH, Q1_FROM, Q1_TO, 10);

        assertEquals(2, top.size());
        assertEquals("Ivan", top.get(0).firstName());
        assertMoney("10000", top.get(0).totalSpent());
        assertEquals("Olena", top.get(1).firstName());
        assertMoney("9200", top.get(1).totalSpent());
    }

    @Test
    @DisplayName("4 (ДЗ) · native SQL дає той самий результат")
    void nativeQueryGivesTheSameResult() {
        List<CustomerSpending> jpql = repository.topCustomersBySpending(CurrencyCode.UAH, Q1_FROM, Q1_TO, 10);
        List<CustomerSpending> sql = repository.topCustomersBySpendingNative(CurrencyCode.UAH, Q1_FROM, Q1_TO, 10);

        assertEquals(jpql.size(), sql.size());
        for (int i = 0; i < jpql.size(); i++) {
            assertEquals(jpql.get(i).customerId(), sql.get(i).customerId());
            assertMoney(jpql.get(i).totalSpent().toPlainString(), sql.get(i).totalSpent());
        }
    }

    @Test
    @DisplayName("3 (ДЗ) · limit обмежує кількість")
    void limitIsApplied() {
        List<CustomerSpending> top = repository.topCustomersBySpending(CurrencyCode.UAH, Q1_FROM, Q1_TO, 1);

        assertEquals(1, top.size());
        assertEquals("Ivan", top.get(0).firstName());
    }

    // ---------- Резерв ----------

    @Test
    @DisplayName("5 (резерв) · фільтр: рахунок + тип + мін. сума")
    void filterByAccountTypeAndMinAmount() {
        TransactionFilter filter = TransactionFilter.empty()
                .withAccount("UA01")
                .withType(TransactionType.DEBIT)
                .withMinAmount("1000");

        List<TransactionView> found = repository.search(filter);

        // 1500, 3500, 1000, 2500
        assertEquals(4, found.size());
    }

    @Test
    @DisplayName("5 (резерв) · фільтр лише за валютою")
    void filterByCurrencyOnly() {
        assertEquals(2, repository.search(TransactionFilter.empty().withCurrency(CurrencyCode.USD)).size());
    }

    @Test
    @DisplayName("5 (резерв) · порожній фільтр — усі операції")
    void emptyFilterReturnsEverything() {
        assertEquals(14, repository.search(TransactionFilter.empty()).size());
    }

    @Test
    @DisplayName("6 (резерв) · накопичений залишок (window function)")
    void runningBalanceEndsWithTotalMovement() {
        List<BalancePoint> points = repository.runningBalance("UA01");

        assertEquals(7, points.size());
        assertMoney("20000", points.get(0).balanceAfter());
        assertMoney("18500", points.get(1).balanceAfter());
        assertMoney("30800", points.get(6).balanceAfter());
    }

    @Test
    @DisplayName("7 (резерв) · bulk update йде повз Persistence Context")
    void bulkUpdateBypassesPersistenceContext() {
        try (EntityManager em = emf.createEntityManager()) {
            Account loadedBefore = em.createQuery("select a from Account a where a.number = 'UA01'", Account.class)
                    .getSingleResult();

            int updated = repository.chargeMonthlyFee(CurrencyCode.UAH, new BigDecimal("50"));

            assertEquals(2, updated, "UA01 і UA03");
            assertMoney("10000", loadedBefore.getBalance()); // старе значення в пам'яті
            em.refresh(loadedBefore);
            assertMoney("9950", loadedBefore.getBalance());  // актуальне значення з бази
        }
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Очікували " + expected + ", а маємо " + actual.toPlainString());
    }
}
