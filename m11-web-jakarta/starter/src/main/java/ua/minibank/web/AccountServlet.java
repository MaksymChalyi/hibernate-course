package ua.minibank.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.minibank.api.ErrorDto;

import java.io.IOException;

/**
 * GET /accounts/{id}                          — рахунок
 * GET /accounts/{id}/transactions?page=&size= — історія операцій (резерв, завдання 6)
 *
 * Servlet лише розбирає HTTP і викликає сервіс. EntityManager і JPQL тут НЕ з'являються —
 * вони живуть у сервісі (AccountService), який повертає готові DTO.
 */
public class AccountServlet extends JsonServlet {

    /*
     * Завдання 3. GET /accounts/{id}.
     *
     * Servlet змаплено на "/accounts/*" (див. AppServer). Тоді для URL /accounts/1
     * request.getPathInfo() поверне "/1", а для /accounts/1/transactions — "/1/transactions".
     *
     * Крок 1. Розбити шлях на частини:
     *     String path = request.getPathInfo();                               // "/1" або null для "/accounts"
     *     String[] parts = path == null ? new String[0] : path.substring(1).split("/");
     *                                                                          // "/1" → ["1"]
     *
     * Крок 2. Одна частина → рахунок, інакше 404:
     *     if (parts.length == 1 && !parts[0].isEmpty()) {
     *         handle(response, 200, () -> app().accountService().getAccount(parseId(parts[0])));
     *     } else {
     *         writeJson(response, 404, new ErrorDto("NOT_FOUND", "Невідомий шлях"));
     *     }
     *
     * Допоміжні parseId і intParam уже написані внизу класу.
     *
     * Перевірка: тести «1–3 · GET /accounts/1 …» і «1–3 · у JSON немає полів entity». Або в браузері:
     *   http://localhost:8080/accounts/1
     *
     * Питання: чому servlet не відкриває EntityManager сам? (Підказка: LazyInitializationException, модуль 6.)
     *
     * ---
     * Завдання 6 (резерв). GET /accounts/{id}/transactions?page=0&size=20.
     * Додай гілку між if і else:
     *     } else if (parts.length == 2 && parts[1].equals("transactions")) {
     *         handle(response, 200, () -> app().accountService().history(
     *                 parseId(parts[0]),
     *                 intParam(request, "page", 0),
     *                 intParam(request, "size", 20)));
     *     }
     * Перевірка: тест «6 (резерв) · історія сторінками, нові спочатку».
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO 3 Розібрати getPathInfo() і повернути рахунок або 404 (кроки в коментарі вище)
        // TODO 6 (резерв) Гілка "/{id}/transactions" з page і size
        writeJson(response, 501, new ErrorDto("NOT_IMPLEMENTED", "TODO 3: AccountServlet — див. коментар над методом"));
    }

    /** "1" → 1L; "abc" → IllegalArgumentException (після завдання 5 це стане відповіддю 400). */
    private static Long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некоректний id: " + value);
        }
    }

    /** Параметр запиту ?name=... як int, або defaultValue, якщо параметра немає. */
    private static int intParam(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Параметр " + name + " має бути числом");
        }
    }
}
