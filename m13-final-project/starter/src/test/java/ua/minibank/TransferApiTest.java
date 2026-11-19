package ua.minibank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тести через справжній HTTP на справжньому PostgreSQL.
 * ApiTestBase (готовий) запускає сервер на випадковому порту і перед кожним тестом відновлює демо-дані з V2:
 *   рахунок 1: UA01, UAH, 10000 (Olena) · рахунок 2: UA02, USD, 500 (Olena) · рахунок 3: UA03, UAH, 3000 (Ivan)
 *
 * Перший тест — зразок. Решту напиши сам (етап 4 у ЗАВДАННЯ.md).
 */
@DisplayName("Модуль 13 · Перекази через API")
class TransferApiTest extends ApiTestBase {

    @Test
    @DisplayName("Зразок · переказ змінює баланси")
    void transferMovesMoney() throws Exception {
        HttpResponse<String> response = postJson("/transfers", """
                {"fromAccountId": 1, "toAccountId": 3, "amount": 250}
                """);

        assertEquals(200, response.statusCode(), response.body());
        assertMoney("9750", balance(1));
        assertMoney("3250", balance(3));
    }

    // TODO 4.1 Тест: бракує коштів → 422 "INSUFFICIENT_FUNDS", баланс не змінився
    // TODO 4.2 Тест: різні валюти (рахунки 1 і 2) → 422
    // TODO 4.3 Тест: кривий JSON, від'ємна сума, переказ на той самий рахунок → 400
    // TODO 4.4 Тест: 40 паралельних переказів по 10 з рахунку 1 на 3 → 9600 і 3400 (шаблон — модуль 12, завдання 6)
}
