package ua.minibank;

import org.junit.jupiter.api.Test;
import ua.minibank.service.StatementImportService;

import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementImportApiTest extends ApiTestBase {

    @Test
    void importsAllRows() throws Exception {
        HttpResponse<String> response = post("/statements", "text/csv", csv(1_000));

        assertEquals(200, response.statusCode(), response.body());
        assertEquals(1_000, json(response).get("rows").asLong());
        assertEquals(1_000, rowsInDb());
    }

    @Test
    void badLineRollsBackWholeFile() throws Exception {
        String body = csv(120) + "UA000999;not-a-date;10.00;UAH;Broken\n";

        HttpResponse<String> response = post("/statements", "text/csv", body);

        assertEquals(400, response.statusCode());
        assertTrue(json(response).get("message").asText().contains("Рядок 122"));
        assertEquals(0, rowsInDb(), "Жоден рядок не має лишитися після помилки");
    }

    @Test
    void wrongHeaderIsRejected() throws Exception {
        assertEquals(400, post("/statements", "text/csv", "a;b;c\n").statusCode());
    }

    private static String csv(int rows) {
        StringBuilder sb = new StringBuilder(StatementImportService.HEADER).append('\n');
        for (int i = 0; i < rows; i++) {
            sb.append(String.format("UA%06d;2026-03-%02d;%d.50;UAH;Операція %d%n", i % 50, 1 + i % 28, i, i));
        }
        return sb.toString();
    }

    private static long rowsInDb() throws Exception {
        try (Connection c = DriverManager.getConnection(DB.url(), DB.user(), DB.password());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("select count(*) from statement_lines")) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
