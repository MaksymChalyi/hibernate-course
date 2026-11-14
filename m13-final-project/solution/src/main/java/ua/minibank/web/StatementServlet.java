package ua.minibank.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * POST /statements — тіло запиту: CSV-виписка (Content-Type: text/csv).
 *   curl -X POST http://localhost:8080/statements -H "Content-Type: text/csv" --data-binary @statement.csv
 */
public class StatementServlet extends JsonServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        handle(response, 200, () -> app().statementImportService().importCsv(request.getReader()));
    }
}
