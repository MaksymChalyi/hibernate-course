package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FetchingTest {

    private static final int CUSTOMERS = 20;

    private static EntityManagerFactory emf;
    private static Statistics stats;
    private final AccountReportService service = new AccountReportService(emf);

    @BeforeAll
    static void start() {
        emf = Persistence.createEntityManagerFactory("minibank");
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        DataSeeder.seed(emf, CUSTOMERS);
    }

    @AfterAll
    static void stop() {
        emf.close();
    }

    @BeforeEach
    void resetCounters() {
        stats.clear();
    }

    // ---------- Обов'язкова частина ----------

    @Test
    void accountOwnerLinesUsesOneQuery() {
        List<String> lines = service.accountOwnerLines();

        assertEquals(CUSTOMERS * DataSeeder.ACCOUNTS_PER_CUSTOMER, lines.size());
        assertEquals("UA00011 — Client1 Surname1", lines.get(0));
        assertEquals(1, stats.getPrepareStatementCount(), "N+1: власник має прийти тим самим запитом");
    }

    @Test
    void transactionHistoryUsesOneQueryAndIsSortedNewestFirst() {
        Long customerId = firstCustomerId();
        stats.clear();

        List<TransactionView> history = service.transactionHistory(customerId);

        assertEquals(DataSeeder.ACCOUNTS_PER_CUSTOMER * DataSeeder.TRANSACTIONS_PER_ACCOUNT, history.size());
        assertEquals(1, stats.getPrepareStatementCount());
        for (int i = 1; i < history.size(); i++) {
            assertTrue(!history.get(i).createdAt().isAfter(history.get(i - 1).createdAt()), "Нові операції мають бути зверху");
        }
    }

    @Test
    void lazyCollectionOutsideEntityManagerThrows() {
        Long customerId = firstCustomerId();
        Customer detached;
        try (EntityManager em = emf.createEntityManager()) {
            detached = em.find(Customer.class, customerId);
        }

        assertThrows(LazyInitializationException.class, () -> detached.getAccounts().size(),
                "Колекція не завантажена, а EntityManager уже закритий");
    }

    @Test
    void customersWithAccountsAreUsableAfterEntityManagerIsClosed() {
        List<Customer> customers = service.customersWithAccounts();

        assertEquals(CUSTOMERS, customers.size(), "Дублікати клієнтів після JOIN з колекцією");
        assertEquals(DataSeeder.ACCOUNTS_PER_CUSTOMER, customers.get(0).getAccounts().size());
        assertEquals(1, stats.getPrepareStatementCount());
    }

    // ---------- Резерв ----------

    @Test
    void entityGraphLoadsOwnersInOneQuery() {
        List<String> lines = service.accountOwnerLinesWithEntityGraph();

        assertEquals(CUSTOMERS * DataSeeder.ACCOUNTS_PER_CUSTOMER, lines.size());
        assertEquals(1, stats.getPrepareStatementCount());
    }

    @Test
    void dtoProjectionUsesOneQuery() {
        List<AccountOwnerView> views = service.accountOwnerViews();

        assertEquals(CUSTOMERS * DataSeeder.ACCOUNTS_PER_CUSTOMER, views.size());
        assertEquals("UA00011 — Client1 Surname1", views.get(0).line());
        assertEquals(1, stats.getPrepareStatementCount());
    }

    private Long firstCustomerId() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select min(c.id) from Customer c", Long.class).getSingleResult();
        }
    }
}
