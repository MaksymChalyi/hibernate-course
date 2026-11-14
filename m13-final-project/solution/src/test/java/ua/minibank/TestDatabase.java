package ua.minibank;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Де виконуються тести:
 *   - Docker є і запущений → Testcontainers піднімає новий PostgreSQL у контейнері;
 *   - Docker немає           → локальний PostgreSQL (localhost:5432, база minibank), окрема схема для тестів.
 *
 * Примусово локальна база: VM option -Dminibank.tests.local=true
 *
 * В обох випадках це справжній PostgreSQL, а не H2 — у цьому головна ідея модуля.
 * Контейнер дає повну ізоляцію і не залежить від того, що встановлено на машині;
 * локальна база — запасний варіант, поки Docker ще не вивчений.
 */
public record TestDatabase(String url, String user, String password, String schema, boolean container) {

    private static final String IMAGE = "postgres:16-alpine";

    /** Викликати один раз на запуск тестів (зі static-блоку базового класу). */
    public static TestDatabase start(String localSchema) {
        boolean forceLocal = Boolean.getBoolean("minibank.tests.local");
        if (!forceLocal && dockerAvailable()) {
            PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(IMAGE);
            postgres.start(); // зупинить службовий контейнер Ryuk після завершення JVM
            System.out.println("[TestDatabase] PostgreSQL у Docker: " + postgres.getJdbcUrl());
            return new TestDatabase(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), "public", true);
        }
        String url = "jdbc:postgresql://localhost:5432/minibank?currentSchema=" + localSchema;
        System.out.println("[TestDatabase] Docker недоступний або вимкнений — локальний PostgreSQL, схема " + localSchema);
        return new TestDatabase(url, "minibank", "minibank", localSchema, false);
    }

    private static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
