package ua.minibank.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.minibank.api.ErrorDto;

import java.io.IOException;

/** POST /transfers з тілом {"fromAccountId": 1, "toAccountId": 3, "amount": 250} */
public class TransferServlet extends JsonServlet {

    /*
     * Завдання 4. Переказ через HTTP.
     *
     * Імпорти: ua.minibank.api.TransferRequest, ua.minibank.api.TransferResult
     *
     * Що робить метод:
     *   1. JSON з тіла запиту → об'єкт TransferRequest (Jackson робить це за нас: readJson).
     *   2. Виклик сервісу з модуля 4 (сервіс сам відкриває транзакцію).
     *   3. Відповідь 200 з TransferResult.
     *
     * Усе — всередині handle(...): він перехопить виняток і перетворить його на JSON-помилку.
     *
     *     handle(response, 200, () -> {
     *         TransferRequest transfer = readJson(request, TransferRequest.class);
     *         app().transferService().transfer(transfer.fromAccountId(), transfer.toAccountId(), transfer.amount());
     *         return TransferResult.completed(transfer);
     *     });
     *
     * Перевірка: тест «4 · POST /transfers переказує гроші». Або в терміналі (Main запущений):
     *   curl -X POST http://localhost:8080/transfers -H "Content-Type: application/json" -d "{\"fromAccountId\":1,\"toAccountId\":3,\"amount\":250}"
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO 4 Прочитати TransferRequest, викликати transferService, повернути TransferResult (код вище)
        writeJson(response, 501, new ErrorDto("NOT_IMPLEMENTED", "TODO 4: TransferServlet — див. коментар над методом"));
    }
}
