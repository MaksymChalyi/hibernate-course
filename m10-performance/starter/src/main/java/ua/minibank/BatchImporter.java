package ua.minibank;

import jakarta.persistence.EntityManagerFactory;

import java.nio.file.Path;

/**
 * TODO 3. Імпорт пачками. Візьми за основу NaiveImporter і зміни одне:
 * кожні batchSize рядків викликай em.flush() і em.clear().
 *   - що робить flush() і що робить clear()?
 *   - що буде, якщо викликати clear() без flush()?
 * Не забудь rollback при помилці. Ім'я в ImportResult — "batch".
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
        throw new UnsupportedOperationException("TODO: BatchImporter");
    }
}
