package ua.minibank.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.minibank.api.TransferRequest;
import ua.minibank.api.TransferResult;

import java.io.IOException;

/** POST /transfers з тілом {"fromAccountId": 1, "toAccountId": 3, "amount": 250} */
public class TransferServlet extends JsonServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(response, 200, () -> {
            TransferRequest transfer = readJson(request, TransferRequest.class);
            app().transferService().transfer(transfer.fromAccountId(), transfer.toAccountId(), transfer.amount());
            return TransferResult.completed(transfer);
        });
    }
}
