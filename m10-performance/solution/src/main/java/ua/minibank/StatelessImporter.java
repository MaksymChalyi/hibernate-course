package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.stat.Statistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Резерв. StatelessSession — Hibernate без Persistence Context:
 * немає first-level cache, dirty checking, cascade і lazy loading.
 * insert() одразу виконує INSERT. Ідеально для масового імпорту, де об'єкт після вставки не потрібен.
 */
public class StatelessImporter implements Importer {

    private final EntityManagerFactory emf;

    public StatelessImporter(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public ImportResult importFile(Path csv) {
        Statistics stats = Emfs.statistics(emf);
        stats.clear();
        long started = System.currentTimeMillis();
        long rows = 0;

        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        StatelessSession session = sessionFactory.openStatelessSession();
        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            Transaction tx = session.beginTransaction();
            try {
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    session.insert(Csv.parse(line));
                    rows++;
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
        } finally {
            session.close();
        }

        return new ImportResult("stateless", rows, System.currentTimeMillis() - started, stats.getPrepareStatementCount());
    }
}
