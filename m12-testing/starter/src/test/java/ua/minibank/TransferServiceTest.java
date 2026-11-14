package ua.minibank;

import org.junit.jupiter.api.Test;
import ua.minibank.service.Tx;
import ua.minibank.service.TransferService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class TransferServiceTest extends PostgresTestBase {

    private final TransferService service = new TransferService(EMF);

    /** Приклад готового тесту. Інші напиши за тим самим шаблоном: дані → дія → перевірка. */
    @Test
    void transferMovesMoneyAndWritesTwoTransactions() {
        TestData.customer().account("UA01", "1000").account("UA02", "100").save(EMF);

        service.transfer(TestData.accountId(EMF, "UA01"), TestData.accountId(EMF, "UA02"), new BigDecimal("250"));

        assertMoney("750", TestData.balance(EMF, "UA01"));
        assertMoney("350", TestData.balance(EMF, "UA02"));
        assertEquals(2L, transactionCount());
    }

    /**
     * TODO 4. На рахунку 100, переказуємо 100.01:
     *   - очікуємо InsufficientFundsException (assertThrows);
     *   - баланси не змінилися;
     *   - записів Transaction немає.
     */
    @Test
    void insufficientFundsChangesNothing() {
        fail("TODO");
    }

    /** TODO 5. Рахунки в UAH і USD — переказ має бути відхилений. */
    @Test
    void differentCurrenciesAreRejected() {
        fail("TODO");
    }

    /**
     * TODO 6. 50 паралельних переказів по 10 з рахунку з 1000 на рахунок з 0
     * (ExecutorService на 8 потоків). Після завершення: 500 і 500.
     * Питання: чому такий тест на H2 нічого б не довів?
     */
    @Test
    void concurrentTransfersKeepBalanceCorrect() {
        fail("TODO");
    }

    private long transactionCount() {
        return Tx.read(EMF, em -> em.createQuery("select count(t) from Transaction t", Long.class).getSingleResult());
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Очікували " + expected + ", а маємо " + actual.toPlainString());
    }
}
