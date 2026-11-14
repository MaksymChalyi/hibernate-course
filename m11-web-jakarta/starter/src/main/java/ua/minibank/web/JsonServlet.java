package ua.minibank.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.minibank.api.ErrorDto;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Базовий клас: читання/запис JSON і перетворення винятків на HTTP-статуси.
 */
public abstract class JsonServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(JsonServlet.class.getName());

    @FunctionalInterface
    protected interface Action {
        Object run() throws Exception;
    }

    protected AppContext app() {
        return AppContext.from(getServletContext());
    }

    protected <T> T readJson(HttpServletRequest request, Class<T> type) throws IOException {
        return app().mapper().readValue(request.getReader(), type);
    }

    protected void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        app().mapper().writeValue(response.getWriter(), body);
    }

    /** Виконує дію і відповідає JSON: результат зі статусом successStatus або помилка. */
    protected void handle(HttpServletResponse response, int successStatus, Action action) throws IOException {
        try {
            writeJson(response, successStatus, action.run());
        } catch (Exception e) {
            writeError(response, e);
        }
    }

    /**
     * TODO 5 (резерв). Зараз будь-яка помилка — 500. Зроби розумні статуси:
     *   AccountNotFoundException                          → 404, "ACCOUNT_NOT_FOUND"
     *   InsufficientFundsException                        → 422, "INSUFFICIENT_FUNDS"
     *   CurrencyMismatchException                         → 422, "CURRENCY_MISMATCH"
     *   IllegalArgumentException, JacksonException (JSON) → 400, "BAD_REQUEST"
     *   OptimisticLockException (будь-де в ланцюжку причин) → 409, "CONCURRENT_UPDATE"
     * Для 500 не віддавай e.getMessage() клієнту. Чому?
     */
    private void writeError(HttpServletResponse response, Exception e) throws IOException {
        LOG.log(Level.SEVERE, "Unexpected error", e);
        writeJson(response, 500, new ErrorDto("INTERNAL_ERROR", "Внутрішня помилка"));
    }
}
