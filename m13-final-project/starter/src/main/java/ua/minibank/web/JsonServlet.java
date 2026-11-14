package ua.minibank.web;

import com.fasterxml.jackson.core.JacksonException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.minibank.api.ErrorDto;
import ua.minibank.domain.AccountNotFoundException;
import ua.minibank.domain.CurrencyMismatchException;
import ua.minibank.domain.InsufficientFundsException;

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
     * Резерв: бізнес-помилка → зрозумілий статус і код. Невідома помилка → 500 без деталей:
     * текст винятку може містити SQL чи імена таблиць, клієнту їх бачити не треба.
     */
    private void writeError(HttpServletResponse response, Exception e) throws IOException {
        if (e instanceof AccountNotFoundException) {
            writeJson(response, 404, new ErrorDto("ACCOUNT_NOT_FOUND", e.getMessage()));
        } else if (e instanceof InsufficientFundsException) {
            writeJson(response, 422, new ErrorDto("INSUFFICIENT_FUNDS", e.getMessage()));
        } else if (e instanceof CurrencyMismatchException) {
            writeJson(response, 422, new ErrorDto("CURRENCY_MISMATCH", e.getMessage()));
        } else if (e instanceof IllegalArgumentException || e instanceof JacksonException) {
            writeJson(response, 400, new ErrorDto("BAD_REQUEST", e.getMessage()));
        } else if (isOptimisticLockFailure(e)) {
            writeJson(response, 409, new ErrorDto("CONCURRENT_UPDATE", "Дані змінилися, повторіть запит"));
        } else {
            LOG.log(Level.SEVERE, "Unexpected error", e);
            writeJson(response, 500, new ErrorDto("INTERNAL_ERROR", "Внутрішня помилка"));
        }
    }

    private static boolean isOptimisticLockFailure(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof jakarta.persistence.OptimisticLockException) {
                return true;
            }
        }
        return false;
    }
}
