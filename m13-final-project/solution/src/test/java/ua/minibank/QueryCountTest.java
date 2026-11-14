package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ua.minibank.service.AccountService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Захист від N+1: кількість SQL не залежить від кількості операцій.
 * Окрема EntityManagerFactory з увімкненою статистикою, та сама тестова база.
 */
class QueryCountTest extends ApiTestBase {

    private static EntityManagerFactory emf;
    private static Statistics stats;

    @BeforeAll
    static void start() {
        emf = Persistence.createEntityManagerFactory("minibank", Map.of(
                "jakarta.persistence.jdbc.url", DB.url(),
                "jakarta.persistence.jdbc.user", DB.user(),
                "jakarta.persistence.jdbc.password", DB.password(),
                "hibernate.default_schema", DB.schema(),
                "hibernate.generate_statistics", "true"));
        stats = emf.unwrap(SessionFactory.class).getStatistics();
    }

    @AfterAll
    static void stop() {
        emf.close();
    }

    @Test
    void accountIsLoadedWithOneQuery() {
        stats.clear();

        new AccountService(emf).getAccount(1L);

        assertEquals(1, stats.getPrepareStatementCount());
    }

    @Test
    void historyNeedsThreeQueriesRegardlessOfSize() throws Exception {
        for (int i = 0; i < 30; i++) {
            postJson("/transfers", "{\"fromAccountId\":1,\"toAccountId\":3,\"amount\":1}");
        }
        stats.clear();

        new AccountService(emf).history(1L, 0, 50);

        // перевірка рахунку + сторінка + count
        assertEquals(3, stats.getPrepareStatementCount());
    }
}
