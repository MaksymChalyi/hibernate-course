package ua.minibank;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Тест через справжній HTTP: Tomcat на випадковому порту, справжня база.
 * Перед кожним тестом база очищується і міграції виконуються заново.
 */
@DisplayName("Модуль 11 · REST API через HTTP")
class WebApiTest {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private static AppServer server;
    private static String baseUrl;

    @BeforeAll
    static void start() {
        resetDatabase();
        server = AppServer.start(0);
        baseUrl = "http://localhost:" + server.port();
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    @BeforeEach
    void reset() {
        resetDatabase();
    }

    // ---------- Обов'язкова частина ----------

    @Test
    @DisplayName("1–3 · GET /accounts/1 повертає рахунок як DTO")
    void getAccountReturnsDto() throws Exception {
        HttpResponse<String> response = get("/accounts/1");

        assertEquals(200, response.statusCode());
        JsonNode account = JSON.readTree(response.body());
        assertEquals("UA01", account.get("number").asText());
        assertEquals("Olena Koval", account.get("ownerName").asText());
        assertMoney("10000", account.get("balance"));
    }

    @Test
    @DisplayName("1–3 · у JSON немає полів entity (version, customer)")
    void entityFieldsDoNotLeakIntoJson() throws Exception {
        JsonNode account = JSON.readTree(get("/accounts/1").body());

        assertFalse(account.has("version"), "Службове поле entity потрапило в API");
        assertFalse(account.has("customer"), "Entity клієнта потрапила в API");
    }

    @Test
    @DisplayName("4 · POST /transfers переказує гроші")
    void transferMovesMoney() throws Exception {
        HttpResponse<String> response = post("/transfers", """
                {"fromAccountId": 1, "toAccountId": 3, "amount": 250}
                """);

        assertEquals(200, response.statusCode(), response.body());
        assertMoney("9750", JSON.readTree(get("/accounts/1").body()).get("balance"));
        assertMoney("3250", JSON.readTree(get("/accounts/3").body()).get("balance"));
    }

    // ---------- Резерв ----------

    @Test
    @DisplayName("5 (резерв) · невідомий рахунок → 404")
    void unknownAccountReturns404() throws Exception {
        HttpResponse<String> response = get("/accounts/999");

        assertEquals(404, response.statusCode());
        assertEquals("ACCOUNT_NOT_FOUND", JSON.readTree(response.body()).get("error").asText());
    }

    @Test
    @DisplayName("5 (резерв) · бракує коштів → 422, баланс не змінився")
    void insufficientFundsReturns422AndChangesNothing() throws Exception {
        HttpResponse<String> response = post("/transfers", """
                {"fromAccountId": 3, "toAccountId": 1, "amount": 100000}
                """);

        assertEquals(422, response.statusCode());
        assertEquals("INSUFFICIENT_FUNDS", JSON.readTree(response.body()).get("error").asText());
        assertMoney("3000", JSON.readTree(get("/accounts/3").body()).get("balance"));
    }

    @Test
    @DisplayName("5 (резерв) · різні валюти → 422")
    void differentCurrenciesReturn422() throws Exception {
        HttpResponse<String> response = post("/transfers", """
                {"fromAccountId": 1, "toAccountId": 2, "amount": 10}
                """);

        assertEquals(422, response.statusCode());
    }

    @Test
    @DisplayName("5 (резерв) · кривий JSON → 400")
    void malformedJsonReturns400() throws Exception {
        assertEquals(400, post("/transfers", "{not json").statusCode());
    }

    @Test
    @DisplayName("6 (резерв) · історія сторінками, нові спочатку")
    void historyIsPagedNewestFirst() throws Exception {
        post("/transfers", """
                {"fromAccountId": 1, "toAccountId": 3, "amount": 250}
                """);

        HttpResponse<String> response = get("/accounts/1/transactions?page=0&size=1");

        assertEquals(200, response.statusCode());
        JsonNode page = JSON.readTree(response.body());
        assertEquals(2, page.get("total").asLong());
        assertEquals(1, page.get("items").size());
        assertEquals("DEBIT", page.get("items").get(0).get("type").asText());
    }

    // ---------- helpers ----------

    private static void resetDatabase() {
        AppConfig config = AppConfig.fromSystemProperties();
        Flyway flyway = Flyway.configure()
                .dataSource(config.dbUrl(), config.dbUser(), config.dbPassword())
                .schemas(config.dbSchema())
                .cleanDisabled(false) // clean за замовчуванням вимкнено — щоб випадково не стерти production
                .load();
        flyway.clean();
        flyway.migrate();
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(String path, String json) throws IOException, InterruptedException {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void assertMoney(String expected, JsonNode actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()),
                "Очікували " + expected + ", а маємо " + actual);
    }
}
