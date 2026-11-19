package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Модуль 10 · Імпорт виписки")
class ImportTest {

    private static final int ROWS = 5_000;
    private static final int BATCH_SIZE = 50;

    @TempDir
    static Path tempDir;

    private static Path csv;
    private static EntityManagerFactory plain;
    private static EntityManagerFactory batching;

    @BeforeAll
    static void start() {
        csv = tempDir.resolve("statement.csv");
        Csv.generate(csv, ROWS);
        Emfs.createSchema();
        plain = Emfs.plain();
        batching = Emfs.withBatching(BATCH_SIZE);
    }

    @AfterAll
    static void stop() {
        plain.close();
        batching.close();
    }

    @BeforeEach
    void truncate() {
        try (EntityManager em = plain.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("truncate table statement_lines").executeUpdate();
            em.getTransaction().commit();
        }
    }

    @Test
    @DisplayName("Готове · Csv.parse розбирає рядок")
    void parsesCsvLine() {
        StatementLine line = Csv.parse("UA000001;2026-03-15;-1250.50;UAH;Оренда");

        assertEquals("UA000001", line.getAccountNumber());
        assertEquals(0, new java.math.BigDecimal("-1250.50").compareTo(line.getAmount()));
        assertEquals(CurrencyCode.UAH, line.getCurrency());
    }

    // ---------- Обов'язкова частина ----------

    @Test
    @DisplayName("Демо · наївний імпорт: окремий запит на кожен рядок")
    void naiveImportSendsOneStatementPerRow() {
        ImportResult result = new NaiveImporter(plain).importFile(csv);
        System.out.println(result);

        assertEquals(ROWS, result.rows());
        assertEquals(ROWS, rowsInDb());
        assertTrue(result.statements() >= ROWS, "Без batching кожен INSERT — окремий запит");
    }

    @Test
    @DisplayName("1–3 · batch: запитів у десятки разів менше")
    void batchImportSendsFarFewerStatements() {
        ImportResult result = new BatchImporter(batching, BATCH_SIZE).importFile(csv);
        System.out.println(result);

        assertEquals(ROWS, rowsInDb());
        // ~100 пачок INSERT + ~100 звернень до sequence (allocationSize = 50)
        assertTrue(result.statements() < ROWS / 10,
                "Запитів " + result.statements() + ": batching не працює. Перевір jdbc.batch_size і генератор id");
    }

    // ---------- Резерв ----------

    @Test
    @DisplayName("4 (резерв) · StatelessSession зберігає всі рядки")
    void statelessImportStoresAllRows() {
        ImportResult result = new StatelessImporter(batching).importFile(csv);
        System.out.println(result);

        assertEquals(ROWS, rowsInDb());
    }

    @Test
    @DisplayName("Демо · чистий JDBC batch (потрібне завдання 1: послідовність)")
    void plainJdbcImportStoresAllRows() {
        ImportResult result = new JdbcBatchImporter().importFile(csv);
        System.out.println(result);

        assertEquals(ROWS, rowsInDb());
    }

    private long rowsInDb() {
        try (EntityManager em = plain.createEntityManager()) {
            return em.createQuery("select count(s) from StatementLine s", Long.class).getSingleResult();
        }
    }
}
