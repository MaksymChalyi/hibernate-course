package ua.minibank;

/**
 * Результат імпорту.
 * statements — кількість підготовлених JDBC-запитів (з Hibernate Statistics), -1 якщо невідомо.
 */
public record ImportResult(String importer, long rows, long millis, long statements) {

    @Override
    public String toString() {
        return String.format("%-12s rows=%,d  time=%,d ms  statements=%,d", importer, rows, millis, statements);
    }
}
