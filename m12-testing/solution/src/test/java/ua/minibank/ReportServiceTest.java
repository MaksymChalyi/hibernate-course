package ua.minibank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.minibank.service.ReportService;
import ua.minibank.service.TransactionView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Модуль 12 · ReportService")
class ReportServiceTest extends PostgresTestBase {

    private final ReportService service = new ReportService(EMF);

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

    /**
     * Домашнє завдання: регресійний тест на N+1.
     * Даних достатньо, щоб N+1 був помітним: 10 рахунків по 5 операцій.
     * Без JOIN FETCH тест покаже 11 запитів замість одного.
     */
    @Test
    @DisplayName("7 (ДЗ) · історія завантажується одним запитом (захист від N+1)")
    void historyUsesOneQuery() {
        TestData.CustomerBuilder builder = TestData.customer();
        for (int i = 0; i < 10; i++) {
            builder.accountWithTransactions("UA1" + i, 5);
        }
        Long customerId = builder.save(EMF);
        statistics().clear();

        List<TransactionView> history = service.customerHistory(customerId);

        assertEquals(50, history.size());
        assertEquals(1, statistics().getPrepareStatementCount(),
                "Історія операцій має завантажуватися одним запитом. Перевір JOIN FETCH");
    }
}
