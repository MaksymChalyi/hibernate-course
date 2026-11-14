package ua.minibank;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransferApiTest extends ApiTestBase {

    @Test
    void transferMovesMoney() throws Exception {
        HttpResponse<String> response = postJson("/transfers", """
                {"fromAccountId": 1, "toAccountId": 3, "amount": 250}
                """);

        assertEquals(200, response.statusCode(), response.body());
        assertEquals("COMPLETED", json(response).get("status").asText());
        assertMoney("9750", balance(1));
        assertMoney("3250", balance(3));
    }

    @Test
    void insufficientFundsReturns422() throws Exception {
        HttpResponse<String> response = postJson("/transfers", """
                {"fromAccountId": 3, "toAccountId": 1, "amount": 3000.01}
                """);

        assertEquals(422, response.statusCode());
        assertEquals("INSUFFICIENT_FUNDS", json(response).get("error").asText());
        assertMoney("3000", balance(3));
    }

    @Test
    void differentCurrenciesReturn422() throws Exception {
        assertEquals(422, postJson("/transfers", """
                {"fromAccountId": 1, "toAccountId": 2, "amount": 10}
                """).statusCode());
    }

    @Test
    void invalidRequestsReturn400() throws Exception {
        assertEquals(400, postJson("/transfers", "{broken").statusCode());
        assertEquals(400, postJson("/transfers", """
                {"fromAccountId": 1, "toAccountId": 3, "amount": -5}
                """).statusCode());
        assertEquals(400, postJson("/transfers", """
                {"fromAccountId": 1, "toAccountId": 1, "amount": 5}
                """).statusCode());
    }

    /** 40 паралельних HTTP-запитів: pessimistic lock не дає втратити жодне списання. */
    @Test
    void concurrentTransfersAreConsistent() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<HttpResponse<String>>> futures = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                futures.add(pool.submit(() -> postJson("/transfers", """
                        {"fromAccountId": 3, "toAccountId": 1, "amount": 10}
                        """)));
            }
            for (Future<HttpResponse<String>> future : futures) {
                assertEquals(200, future.get(30, TimeUnit.SECONDS).statusCode());
            }
        } finally {
            pool.shutdownNow();
        }

        assertMoney("2600", balance(3));
        assertMoney("10400", balance(1));
    }
}
