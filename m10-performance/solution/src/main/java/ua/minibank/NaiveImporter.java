package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.stat.Statistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Наївний імпорт: persist кожного рядка в одному EntityManager, один commit наприкінці.
 *
 * Проблеми:
 *   1. Кожен об'єкт лишається в Persistence Context до кінця: 100 000 entity + знімки для dirty checking.
 *      Пам'ять росте лінійно, а flush перед commit перевіряє всі об'єкти.
 *   2. Без jdbc.batch_size кожен INSERT — окремий запит до бази.
 */
public class NaiveImporter implements Importer {

    private final EntityManagerFactory emf;

    public NaiveImporter(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public ImportResult importFile(Path csv) {
        Statistics stats = Emfs.statistics(emf);
        stats.clear();
        long started = System.currentTimeMillis();
        long rows = 0;

        try (EntityManager em = emf.createEntityManager();
             BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            em.getTransaction().begin();
            reader.readLine(); // заголовок
            String line;
            while ((line = reader.readLine()) != null) {
                em.persist(Csv.parse(line));
                rows++;
            }
            em.getTransaction().commit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new ImportResult("naive", rows, System.currentTimeMillis() - started, stats.getPrepareStatementCount());
    }
}
