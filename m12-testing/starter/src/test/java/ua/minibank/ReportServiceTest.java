package ua.minibank;

import org.junit.jupiter.api.Test;
import ua.minibank.service.ReportService;
import ua.minibank.service.TransactionView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ReportServiceTest extends PostgresTestBase {

    private final ReportService service = new ReportService(EMF);

    @Test
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
     * TODO 7 (домашнє завдання). Регресійний тест на N+1:
     *   1. Клієнт з 10 рахунками по 5 операцій (TestData.customer().accountWithTransactions(...)).
     *   2. statistics().clear() — після підготовки даних, перед дією.
     *   3. service.customerHistory(customerId).
     *   4. Перевір: 50 записів і statistics().getPrepareStatementCount() == 1.
     * Потім прибери "fetch" у ReportService і переконайся, що тест падає. Поверни "fetch".
     */
    @Test
    void historyUsesOneQuery() {
        fail("TODO");
    }
}
