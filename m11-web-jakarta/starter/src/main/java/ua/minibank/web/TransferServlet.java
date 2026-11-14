package ua.minibank.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.minibank.api.ErrorDto;

import java.io.IOException;

/** POST /transfers з тілом {"fromAccountId": 1, "toAccountId": 3, "amount": 250} */
public class TransferServlet extends JsonServlet {

    /**
     * TODO 4. Прочитай TransferRequest (readJson), виклич app().transferService().transfer(...)
     * і поверни TransferResult.completed(request). Усе всередині handle(response, 200, () -> ...).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeJson(response, 501, new ErrorDto("NOT_IMPLEMENTED", "TODO: TransferServlet"));
    }
}
