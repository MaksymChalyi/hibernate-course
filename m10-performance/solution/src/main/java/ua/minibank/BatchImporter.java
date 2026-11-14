package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.hibernate.stat.Statistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Імпорт пачками: кожні batchSize рядків — flush() і clear().
 *   flush() відправляє накопичені INSERT у базу (одним JDBC batch, якщо налаштовано jdbc.batch_size);
 *   clear() звільняє Persistence Context — пам'ять не росте.
 * batchSize має дорівнювати hibernate.jdbc.batch_size.
 *
 * Уся виписка — одна транзакція: або імпортовано все, або нічого.
 * Альтернатива — commit кожні N рядків: менше навантаження на базу, але після збою
 * частина рядків уже збережена, і повторний запуск мусить уміти продовжити з місця зупинки.
 */
public class BatchImporter implements Importer {

    private final EntityManagerFactory emf;
    private final int batchSize;

    public BatchImporter(EntityManagerFactory emf, int batchSize) {
        this.emf = emf;
        this.batchSize = batchSize;
    }

    @Override
    public ImportResult importFile(Path csv) {
        Statistics stats = Emfs.statistics(emf);
        stats.clear();
        long started = System.currentTimeMillis();
        long rows = 0;

        try (EntityManager em = emf.createEntityManager();
             BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    em.persist(Csv.parse(line));
                    rows++;
                    if (rows % batchSize == 0) {
                        em.flush();
                        em.clear();
                    }
                }
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new ImportResult("batch", rows, System.currentTimeMillis() - started, stats.getPrepareStatementCount());
    }
}
