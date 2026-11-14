package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Порівняння способів імпорту на 100 000 рядків.
 * Запускати з -Xmx256m, щоб побачити, як наївний імпорт впирається в пам'ять.
 */
public class Main {

    private static final int ROWS = 100_000;
    private static final int BATCH_SIZE = 50;

    public static void main(String[] args) throws Exception {
        Path csv = Path.of("target", "statement.csv");
        Files.createDirectories(csv.getParent());
        Csv.generate(csv, ROWS);
        Emfs.createSchema();

        List<ImportResult> results = new ArrayList<>();
        try (EntityManagerFactory plain = Emfs.plain();
             EntityManagerFactory batching = Emfs.withBatching(BATCH_SIZE)) {

            truncate(plain);
            results.add(new NaiveImporter(plain).importFile(csv));

            truncate(plain);
            results.add(new BatchImporter(batching, BATCH_SIZE).importFile(csv));

            truncate(plain);
            results.add(new StatelessImporter(batching).importFile(csv));

            truncate(plain);
            results.add(new JdbcBatchImporter().importFile(csv));
        }

        System.out.println();
        results.forEach(System.out::println);
    }

    private static void truncate(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("truncate table statement_lines").executeUpdate();
            em.getTransaction().commit();
        }
    }
}
