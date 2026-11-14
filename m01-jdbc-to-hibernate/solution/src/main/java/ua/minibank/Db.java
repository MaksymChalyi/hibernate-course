package ua.minibank;

/**
 * Параметри підключення для JDBC-коду.
 * Hibernate бере ті самі значення з META-INF/persistence.xml.
 */
public final class Db {
    public static final String URL = "jdbc:postgresql://localhost:5432/minibank?currentSchema=m01";
    public static final String USER = "minibank";
    public static final String PASSWORD = "minibank";

    private Db() {
    }
}
