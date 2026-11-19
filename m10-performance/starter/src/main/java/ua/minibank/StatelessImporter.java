package ua.minibank;

import jakarta.persistence.EntityManagerFactory;

import java.nio.file.Path;

/*
 * Завдання 4 (резерв). Імпорт через StatelessSession.
 *
 * StatelessSession — Hibernate БЕЗ Persistence Context: немає кешу, знімків, dirty checking, cascade, lazy loading.
 * session.insert(entity) одразу виконує INSERT (з batching — пакетами). Ідеально для масової вставки,
 * коли об'єкт після вставки не потрібен.
 *
 * Скелет (імпорти: org.hibernate.SessionFactory, org.hibernate.StatelessSession, org.hibernate.Transaction):
 *
 *     Statistics stats = Emfs.statistics(emf);
 *     stats.clear();
 *     long started = System.currentTimeMillis();
 *     long rows = 0;
 *
 *     SessionFactory sf = emf.unwrap(SessionFactory.class);     // «рідна» фабрика Hibernate за JPA-інтерфейсом
 *     StatelessSession session = sf.openStatelessSession();
 *     try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
 *         Transaction tx = session.beginTransaction();
 *         try {
 *             reader.readLine();                               // заголовок
 *             String line;
 *             while ((line = reader.readLine()) != null) {
 *                 session.insert(Csv.parse(line));
 *                 rows++;
 *             }
 *             tx.commit();
 *         } catch (RuntimeException e) {
 *             if (tx.isActive()) tx.rollback();
 *             throw e;
 *         }
 *     } catch (IOException e) {
 *         throw new UncheckedIOException(e);
 *     } finally {
 *         session.close();
 *     }
 *     return new ImportResult("stateless", rows, System.currentTimeMillis() - started, stats.getPrepareStatementCount());
 *
 * Порівняй час з BatchImporter. Чого StatelessSession НЕ вміє? (Список — у лекції.)
 */
// TODO 4 (резерв) Імпорт через StatelessSession (скелет над класом)
public class StatelessImporter implements Importer {

    private final EntityManagerFactory emf;

    public StatelessImporter(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public ImportResult importFile(Path csv) {
        throw new UnsupportedOperationException("TODO 4: StatelessImporter — скелет у коментарі над класом");
    }
}
