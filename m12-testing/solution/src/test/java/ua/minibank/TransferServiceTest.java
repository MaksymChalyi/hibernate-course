package ua.minibank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.minibank.domain.CurrencyCode;
import ua.minibank.domain.CurrencyMismatchException;
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

@DisplayName("Модуль 12 · TransferService на справжньому PostgreSQL")
class TransferServiceTest extends PostgresTestBase {

    private final TransferService service = new TransferService(EMF);

    @Test
    @DisplayName("1–3 · Приклад: переказ змінює баланси і пише 2 операції")
    void transferMovesMoneyAndWritesTwoTransactions() {
        TestData.customer().account("UA01", "1000").account("UA02", "100").save(EMF);

        service.transfer(TestData.accountId(EMF, "UA01"), TestData.accountId(EMF, "UA02"), new BigDecimal("250"));

        assertMoney("750", TestData.balance(EMF, "UA01"));
        assertMoney("350", TestData.balance(EMF, "UA02"));
        assertEquals(2L, transactionCount());
    }

    @Test
    @DisplayName("4 · бракує коштів → виняток, нічого не змінилося")
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
    @DisplayName("5 · різні валюти → виняток")
    void differentCurrenciesAreRejected() {
        TestData.customer().account("UA01", "1000").account("UA02", CurrencyCode.USD, "100").save(EMF);

        assertThrows(CurrencyMismatchException.class, () ->
                service.transfer(TestData.accountId(EMF, "UA01"), TestData.accountId(EMF, "UA02"), BigDecimal.TEN));

        assertMoney("1000", TestData.balance(EMF, "UA01"));
        assertMoney("100", TestData.balance(EMF, "UA02"));
    }

    /** Те, що H2 не перевірить: справжні блокування рядків PostgreSQL. */
    @Test
    @DisplayName("6 · 50 паралельних переказів не губляться")
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
