package ua.minibank.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.minibank.api.ErrorDto;

import java.io.IOException;

/**
 * GET /accounts/{id}                          — рахунок
 * GET /accounts/{id}/transactions?page=&size= — історія операцій (резерв)
 */
public class AccountServlet extends JsonServlet {

    /**
     * TODO 3. request.getPathInfo() для /accounts/1 поверне "/1".
     *   - розбери id;
     *   - handle(response, 200, () -> app().accountService().getAccount(id));
     *   - невідомий шлях → writeJson(response, 404, new ErrorDto("NOT_FOUND", ...)).
     * Зверни увагу: servlet не відкриває EntityManager. Чому це добре?
     *
     * TODO 6 (резерв). Шлях "/1/transactions" з параметрами page (за замовчуванням 0) і size (20)
     * → app().accountService().history(id, page, size).
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeJson(response, 501, new ErrorDto("NOT_IMPLEMENTED", "TODO: AccountServlet"));
    }
}
