package ua.minibank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;

/**
 * Резерв. Той самий імпорт на чистому JDBC batch — орієнтир «наскільки швидко взагалі можна».
 * reWriteBatchedInserts=true: драйвер PostgreSQL склеює пачку в один INSERT ... VALUES (...), (...), ...
 */
public class JdbcBatchImporter implements Importer {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/minibank?currentSchema=m10&reWriteBatchedInserts=true";
    private static final int BATCH = 500;

    @Override
    public ImportResult importFile(Path csv) {
        long started = System.currentTimeMillis();
        long rows = 0;
        String sql = """
                insert into statement_lines (id, account_number, booking_date, amount, currency, description)
                values (nextval('statement_line_seq'), ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DriverManager.getConnection(URL, "minibank", "minibank");
             PreparedStatement ps = connection.prepareStatement(sql);
             BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            connection.setAutoCommit(false);
            try {
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    StatementLine s = Csv.parse(line);
                    ps.setString(1, s.getAccountNumber());
                    ps.setDate(2, Date.valueOf(s.getBookingDate()));
                    ps.setBigDecimal(3, s.getAmount());
                    ps.setString(4, s.getCurrency().name());
                    ps.setString(5, s.getDescription());
                    ps.addBatch();
                    if (++rows % BATCH == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Помилка JDBC-імпорту", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ImportResult("jdbc", rows, System.currentTimeMillis() - started, -1);
    }
}
