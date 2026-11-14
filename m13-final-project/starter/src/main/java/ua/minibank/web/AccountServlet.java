package ua.minibank.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.minibank.api.ErrorDto;

import java.io.IOException;

/**
 * GET /accounts/{id}                         — рахунок
 * GET /accounts/{id}/transactions?page=&size= — історія операцій (резерв)
 *
 * Servlet лише розбирає HTTP і викликає сервіс. Ні EntityManager, ні JPQL тут немає.
 */
public class AccountServlet extends JsonServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo(); // "/1" або "/1/transactions"
        String[] parts = path == null ? new String[0] : path.substring(1).split("/");

        if (parts.length == 1 && !parts[0].isEmpty()) {
            handle(response, 200, () -> app().accountService().getAccount(parseId(parts[0])));
        } else if (parts.length == 2 && parts[1].equals("transactions")) {
            handle(response, 200, () -> app().accountService().history(
                    parseId(parts[0]),
                    intParam(request, "page", 0),
                    intParam(request, "size", 20)));
        } else {
            writeJson(response, 404, new ErrorDto("NOT_FOUND", "Невідомий шлях"));
        }
    }

    private static Long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некоректний id: " + value);
        }
    }

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
