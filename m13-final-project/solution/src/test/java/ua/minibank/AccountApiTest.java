package ua.minibank;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Дані: демо з V2 (рахунок 1 — UA01, 10000 UAH, одна операція CREDIT 10000 від 05.01.2026)
 * плюс три перекази з рахунку 1 на рахунок 3: 100, 2000, 300.
 * Разом на рахунку 1: 1 CREDIT і 3 DEBIT.
 */
class AccountApiTest extends ApiTestBase {

    @BeforeEach
    void makeTransfers() throws Exception {
        for (String amount : new String[]{"100", "2000", "300"}) {
            postJson("/transfers", "{\"fromAccountId\":1,\"toAccountId\":3,\"amount\":" + amount + "}");
        }
    }

    @Test
    void accountIsReturnedAsDto() throws Exception {
        JsonNode account = json(get("/accounts/1"));

        assertEquals("UA01", account.get("number").asText());
        assertEquals("Olena Koval", account.get("ownerName").asText());
        assertMoney("7600", account.get("balance").decimalValue());
        assertFalse(account.has("version"));
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        assertEquals(404, get("/accounts/999").statusCode());
    }

    @Test
    void historyWithoutFiltersIsPaged() throws Exception {
        JsonNode page = json(get("/accounts/1/transactions?page=0&size=2"));

        assertEquals(4, page.get("total").asLong());
        assertEquals(2, page.get("items").size());
    }

    @Test
    void filterByType() throws Exception {
        assertEquals(3, json(get("/accounts/1/transactions?type=DEBIT")).get("total").asLong());
        assertEquals(1, json(get("/accounts/1/transactions?type=credit")).get("total").asLong());
    }

    @Test
    void filterByAmount() throws Exception {
        JsonNode page = json(get("/accounts/1/transactions?type=DEBIT&minAmount=500"));

        assertEquals(1, page.get("total").asLong());
        assertMoney("2000", page.get("items").get(0).get("amount").decimalValue());
    }

    @Test
    void filterByDatesIncludesWholeLastDay() throws Exception {
        // Початкова операція — 05.01.2026; перекази — сьогодні
        JsonNode page = json(get("/accounts/1/transactions?from=2026-01-01&to=2026-01-05"));

        assertEquals(1, page.get("total").asLong());
    }

    @Test
    void badFilterReturns400() throws Exception {
        HttpResponse<String> badType = get("/accounts/1/transactions?type=REFUND");
        HttpResponse<String> badDate = get("/accounts/1/transactions?from=01.01.2026");
        HttpResponse<String> hugePage = get("/accounts/1/transactions?size=10000");

        assertEquals(400, badType.statusCode());
        assertEquals(400, badDate.statusCode());
        assertEquals(400, hugePage.statusCode());
    }
}
