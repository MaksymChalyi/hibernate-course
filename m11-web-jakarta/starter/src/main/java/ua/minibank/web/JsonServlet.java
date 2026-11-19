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

    /*
     * Завдання 5 (резерв). Правильні HTTP-статуси для помилок.
     *
     * Зараз будь-яка помилка → 500 «Внутрішня помилка». Клієнт (мобільний застосунок, фронтенд) не може
     * відрізнити «немає такого рахунку» від «впала база». Треба:
     *
     *   Виняток                                              Статус  Код у JSON
     *   AccountNotFoundException                             404     "ACCOUNT_NOT_FOUND"
     *   InsufficientFundsException                           422     "INSUFFICIENT_FUNDS"
     *   CurrencyMismatchException                            422     "CURRENCY_MISMATCH"
     *   IllegalArgumentException або JacksonException        400     "BAD_REQUEST"      (кривий JSON, id = "abc")
     *   OptimisticLockException будь-де в ланцюжку getCause() 409     "CONCURRENT_UPDATE"
     *   усе інше                                             500     "INTERNAL_ERROR"   (як зараз)
     *
     * Імпорти: com.fasterxml.jackson.core.JacksonException, ua.minibank.domain.AccountNotFoundException,
     *          ua.minibank.domain.InsufficientFundsException, ua.minibank.domain.CurrencyMismatchException
     *
     * Шаблон:
     *     if (e instanceof AccountNotFoundException) {
     *         writeJson(response, 404, new ErrorDto("ACCOUNT_NOT_FOUND", e.getMessage()));
     *     } else if (e instanceof InsufficientFundsException) {
     *         ...
     *     } else if (isOptimisticLockFailure(e)) {
     *         writeJson(response, 409, new ErrorDto("CONCURRENT_UPDATE", "Дані змінилися, повторіть запит"));
     *     } else {
     *         LOG.log(Level.SEVERE, "Unexpected error", e);       // повний stack trace — у лог, для нас
     *         writeJson(response, 500, new ErrorDto("INTERNAL_ERROR", "Внутрішня помилка"));  // клієнту — без деталей
     *     }
     *
     * Чому OptimisticLockException шукаємо в ланцюжку: Hibernate часто загортає його в RollbackException.
     *     private static boolean isOptimisticLockFailure(Throwable error) {
     *         for (Throwable t = error; t != null; t = t.getCause()) {
     *             if (t instanceof jakarta.persistence.OptimisticLockException) return true;
     *         }
     *         return false;
     *     }
     *
     * Питання: чому для 500 не можна віддавати клієнту e.getMessage()?
     *
     * Перевірка: тести з позначкою «5 (резерв)».
     */
    // TODO 5 (резерв) Бізнес-винятки → 404/422/400/409 замість 500 (таблиця в коментарі вище)
    private void writeError(HttpServletResponse response, Exception e) throws IOException {
        LOG.log(Level.SEVERE, "Unexpected error", e);
        writeJson(response, 500, new ErrorDto("INTERNAL_ERROR", "Внутрішня помилка"));
    }
}
