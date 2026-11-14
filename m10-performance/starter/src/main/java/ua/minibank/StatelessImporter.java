package ua.minibank;

import jakarta.persistence.EntityManagerFactory;

import java.nio.file.Path;

/**
 * TODO 4 (резерв). Імпорт через StatelessSession:
 *   SessionFactory sf = emf.unwrap(SessionFactory.class);
 *   StatelessSession session = sf.openStatelessSession();
 *   try {
 *       Transaction tx = session.beginTransaction();   // org.hibernate.Transaction
 *       ... session.insert(entity) ...
 *       tx.commit();
 *   } finally {
 *       session.close();
 *   }
 * Порівняй час з BatchImporter. Чого StatelessSession не вміє?
 */
public class StatelessImporter implements Importer {

    private final EntityManagerFactory emf;

    public StatelessImporter(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public ImportResult importFile(Path csv) {
        throw new UnsupportedOperationException("TODO: StatelessImporter");
    }
}
