package ua.minibank.service;

import jakarta.persistence.EntityManagerFactory;
import ua.minibank.api.ImportResultDto;
import ua.minibank.domain.StatementLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

/**
 * Імпорт виписки пачками (модуль 10). Одна транзакція на файл:
 * помилка в будь-якому рядку — відкат усього файлу і HTTP 400 з номером рядка.
 */
public class StatementImportService {

    public static final String HEADER = "account_number;booking_date;amount;currency;description";

    /** Має збігатися з hibernate.jdbc.batch_size у persistence.xml. */
    private static final int BATCH_SIZE = 50;

    private final EntityManagerFactory emf;

    public StatementImportService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public ImportResultDto importCsv(Reader csv) {
        long started = System.currentTimeMillis();
        BufferedReader reader = new BufferedReader(csv);

        long rows = Tx.inTransaction(emf, em -> {
            try {
                String header = reader.readLine();
                if (!HEADER.equals(header)) {
                    throw new IllegalArgumentException("Очікували заголовок: " + HEADER);
                }
                long count = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    em.persist(StatementLine.parse(line, count + 2));
                    count++;
                    if (count % BATCH_SIZE == 0) {
                        em.flush();
                        em.clear();
                    }
                }
                return count;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        return new ImportResultDto(rows, System.currentTimeMillis() - started);
    }
}
