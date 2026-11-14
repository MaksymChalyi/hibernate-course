package ua.minibank;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Інтеграційні тести через HTTP: справжній PostgreSQL + застосунок на випадковому порту.
 * База (Docker-контейнер або локальна схема m13_test, див. TestDatabase) і сервер готуються один раз на весь запуск тестів.
 */
public abstract class ApiTestBase {

    protected static final TestDatabase DB = TestDatabase.start("m13_test");
    protected static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    protected static final AppServer SERVER;

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String BASE_URL;

    static {
        // Застосунок читає підключення з system properties (AppConfig)
        System.setProperty("minibank.db.url", DB.url());
        System.setProperty("minibank.db.user", DB.user());
        System.setProperty("minibank.db.password", DB.password());
        System.setProperty("minibank.db.schema", DB.schema());

        SERVER = AppServer.start(0);
        BASE_URL = "http://localhost:" + SERVER.port();
    }

    /** Кожен тест — з чистою базою і демо-даними з V2. */
    @BeforeEach
    void resetDatabase() {
        Flyway flyway = Flyway.configure()
                .dataSource(DB.url(), DB.user(), DB.password())
                .schemas(DB.schema())
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
    }

    protected static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(BASE_URL + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    protected static HttpResponse<String> postJson(String path, String json) throws IOException, InterruptedException {
        return post(path, "application/json", json);
    }

    protected static HttpResponse<String> post(String path, String contentType, String body)
            throws IOException, InterruptedException {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(BASE_URL + path))
                        .header("Content-Type", contentType)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    protected static JsonNode json(HttpResponse<String> response) throws IOException {
        return JSON.readTree(response.body());
    }

    protected static BigDecimal balance(long accountId) throws Exception {
        return json(get("/accounts/" + accountId)).get("balance").decimalValue();
    }

    protected static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Очікували " + expected + ", а маємо " + actual.toPlainString());
    }
}
