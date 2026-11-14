package ua.minibank;

import org.junit.jupiter.api.Test;
import ua.minibank.domain.CurrencyCode;
import ua.minibank.domain.InsufficientFundsException;
import ua.minibank.service.Tx;
import ua.minibank.service.TransferService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferServiceTest extends PostgresTestBase {

    private final TransferService service = new TransferService(EMF);

    @Test
    void transferMovesMoneyAndWritesTwoTransactions() {
        TestData.customer().account("UA01", "1000").account("UA02", "100").save(EMF);

        service.transfer(TestData.accountId(EMF, "UA01"), TestData.accountId(EMF, "UA02"), new BigDecimal("250"));

        assertMoney("750", TestData.balance(EMF, "UA01"));
        assertMoney("350", TestData.balance(EMF, "UA02"));
        assertEquals(2L, transactionCount());
    }

    @Test
    void insufficientFundsChangesNothing() {
        TestData.customer().account("UA01", "100").account("UA02", "0").save(EMF);
        Long from = TestData.accountId(EMF, "UA01");
        Long to = TestData.accountId(EMF, "UA02");

        assertThrows(InsufficientFundsException.class, () -> service.transfer(from, to, new BigDecimal("100.01")));

        assertMoney("100", TestData.balance(EMF, "UA01"));
        assertMoney("0", TestData.balance(EMF, "UA02"));
        assertEquals(0L, transactionCount());
    }

    @Test
    void differentCurrenciesAreRejected() {
        TestData.customer().account("UA01", "1000").account("UA02", CurrencyCode.USD, "100").save(EMF);

        assertThrows(RuntimeException.class, () ->
                service.transfer(TestData.accountId(EMF, "UA01"), TestData.accountId(EMF, "UA02"), BigDecimal.TEN));
    }

    /** Те, що H2 не перевірить: справжні блокування рядків PostgreSQL. */
    @Test
    void concurrentTransfersKeepBalanceCorrect() throws Exception {
        TestData.customer().account("UA01", "1000").account("UA02", "0").save(EMF);
        Long from = TestData.accountId(EMF, "UA01");
        Long to = TestData.accountId(EMF, "UA02");

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                futures.add(pool.submit(() -> service.transfer(from, to, BigDecimal.TEN)));
            }
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertMoney("500", TestData.balance(EMF, "UA01"));
        assertMoney("500", TestData.balance(EMF, "UA02"));
    }

    private long transactionCount() {
        return Tx.read(EMF, em -> em.createQuery("select count(t) from Transaction t", Long.class).getSingleResult());
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Очікували " + expected + ", а маємо " + actual.toPlainString());
    }
}
