package ua.minibank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.minibank.service.ReportService;
import ua.minibank.service.TransactionView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Модуль 12 · ReportService")
class ReportServiceTest extends PostgresTestBase {

    private final ReportService service = new ReportService(EMF);

    /** Зразок. Стане зеленим після завдань 1–3. */
    @Test
    @DisplayName("1–3 · Приклад: історія лише операцій цього клієнта")
    void historyContainsTransactionsOfAllCustomerAccounts() {
        Long customerId = TestData.customer()
                .accountWithTransactions("UA01", 3)
                .accountWithTransactions("UA02", 2)
                .save(EMF);
        TestData.customer().named("Ivan", "Petrenko").accountWithTransactions("UA03", 4).save(EMF);

        List<TransactionView> history = service.customerHistory(customerId);

        assertEquals(5, history.size(), "Лише операції цього клієнта");
    }

    /*
     * Завдання 7 (домашнє). Тест, що впаде, якщо хтось випадково поверне N+1.
     *
     * Ідея: рахуємо SQL-запити під час дії. Має бути рівно 1 (завдяки join fetch у ReportService).
     *
     *     // дані: 10 рахунків по 5 операцій
     *     TestData.CustomerBuilder builder = TestData.customer();
     *     for (int i = 0; i < 10; i++) {
     *         builder.accountWithTransactions("UA1" + i, 5);
     *     }
     *     Long customerId = builder.save(EMF);
     *
     *     statistics().clear();                   // ВАЖЛИВО: обнулити після підготовки даних, перед дією
     *
     *     // дія
     *     List<TransactionView> history = service.customerHistory(customerId);
     *
     *     // перевірка
     *     assertEquals(50, history.size());
     *     assertEquals(1, statistics().getPrepareStatementCount(),
     *             "Історія операцій має завантажуватися одним запитом. Перевір JOIN FETCH");
     *
     * Експеримент: у ReportService прибери слово fetch ("join fetch t.account a" → "join t.account a").
     * Тест має впасти з «expected 1 but was 11». Поверни fetch.
     */
    @Test
    @DisplayName("7 (ДЗ) · історія завантажується одним запитом (захист від N+1)")
    void historyUsesOneQuery() {
        // TODO 7 (ДЗ) Регресійний тест на кількість запитів (код у коментарі вище)
        fail("TODO 7: напиши тест — див. коментар над методом");
    }
}
