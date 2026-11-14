package ua.minibank;

/**
 * Налаштування підключення. Значення за замовчуванням — для локального PostgreSQL (setup-local-db.sql у корені курсу).
 * Перевизначаються system properties: -Dminibank.db.url=... (так робитимуть тести в модулі 12).
 */
public record AppConfig(String dbUrl, String dbUser, String dbPassword, String dbSchema) {

    public static AppConfig fromSystemProperties() {
        return new AppConfig(
                System.getProperty("minibank.db.url", "jdbc:postgresql://localhost:5432/minibank?currentSchema=m11"),
                System.getProperty("minibank.db.user", "minibank"),
                System.getProperty("minibank.db.password", "minibank"),
                System.getProperty("minibank.db.schema", "m11"));
    }
}
