package ua.minibank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.minibank.service.Tx;
import ua.minibank.service.TransferService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Кожен тест — три блоки:
 *   1) ДАНІ:      TestData.customer().account(...).save(EMF)   — що є в базі до дії;
 *   2) ДІЯ:       service.transfer(...)                         — один виклик того, що перевіряємо;
 *   3) ПЕРЕВІРКА: assertEquals / assertThrows / assertMoney     — що мало статися.
 *
 * Перший тест уже написаний — використовуй його як зразок.
 */
@DisplayName("Модуль 12 · TransferService на справжньому PostgreSQL")
class TransferServiceTest extends PostgresTestBase {

    private final TransferService service = new TransferService(EMF);

    /** Зразок. Стане зеленим після завдань 1–3. */
    @Test
    @DisplayName("1–3 · Приклад: переказ змінює баланси і пише 2 операції")
    void transferMovesMoneyAndWritesTwoTransactions() {
        // дані
        TestData.customer().account("UA01", "1000").account("UA02", "100").save(EMF);

        // дія
        service.transfer(TestData.accountId(EMF, "UA01"), TestData.accountId(EMF, "UA02"), new BigDecimal("250"));

        // перевірка
        assertMoney("750", TestData.balance(EMF, "UA01"));
        assertMoney("350", TestData.balance(EMF, "UA02"));
        assertEquals(2L, transactionCount());
    }

    /*
     * Завдання 4. Переказ більшої суми, ніж є на рахунку, НІЧОГО не змінює.
     *
     * Імпорти: ua.minibank.domain.InsufficientFundsException,
     *          static org.junit.jupiter.api.Assertions.assertThrows
     *
     *     // дані: UA01 = 100, UA02 = 0
     *     TestData.customer().account("UA01", "100").account("UA02", "0").save(EMF);
     *     Long from = TestData.accountId(EMF, "UA01");
     *     Long to = TestData.accountId(EMF, "UA02");
     *
     *     // дія + перевірка винятку: assertThrows виконує лямбду і перевіряє, що вона кинула саме цей виняток
     *     assertThrows(InsufficientFundsException.class, () -> service.transfer(from, to, new BigDecimal("100.01")));
     *
     *     // перевірка: rollback спрацював
     *     assertMoney("100", TestData.balance(EMF, "UA01"));
     *     assertMoney("0", TestData.balance(EMF, "UA02"));
     *     assertEquals(0L, transactionCount());
     *
     * Чому from і to — окремі змінні: у лямбді можна використовувати лише змінні, які не змінюються після присвоєння.
     */
    @Test
    @DisplayName("4 · бракує коштів → виняток, нічого не змінилося")
    void insufficientFundsChangesNothing() {
        // TODO 4 Бракує коштів: assertThrows + баланси без змін + 0 операцій (код у коментарі вище)
        fail("TODO 4: напиши тест — див. коментар над методом");
    }

    /*
     * Завдання 5. Переказ між рахунками в різних валютах відхиляється.
     * Напиши сам за зразком завдання 4:
     *   - дані: .account("UA01", "1000").account("UA02", CurrencyCode.USD, "100")
     *   - очікуваний виняток: CurrencyMismatchException
     *   - баланси після: 1000 і 100
     *
     * Імпорти: ua.minibank.domain.CurrencyCode, ua.minibank.domain.CurrencyMismatchException
     */
    @Test
    @DisplayName("5 · різні валюти → виняток")
    void differentCurrenciesAreRejected() {
        // TODO 5 Різні валюти: CurrencyMismatchException + баланси без змін (за зразком завдання 4)
        fail("TODO 5: напиши тест — див. коментар над методом");
    }

    /*
     * Завдання 6. 50 переказів по 10 ОДНОЧАСНО з 8 потоків. Жоден не має загубитися.
     *
     * Імпорти: java.util.ArrayList, java.util.List, java.util.concurrent.ExecutorService,
     *          java.util.concurrent.Executors, java.util.concurrent.Future, java.util.concurrent.TimeUnit
     * Сигнатура методу: додай "throws Exception" (future.get кидає checked-винятки).
     *
     *     TestData.customer().account("UA01", "1000").account("UA02", "0").save(EMF);
     *     Long from = TestData.accountId(EMF, "UA01");
     *     Long to = TestData.accountId(EMF, "UA02");
     *
     *     ExecutorService pool = Executors.newFixedThreadPool(8);    // 8 потоків виконують задачі паралельно
     *     try {
     *         List<Future<?>> futures = new ArrayList<>();
     *         for (int i = 0; i < 50; i++) {
     *             futures.add(pool.submit(() -> service.transfer(from, to, BigDecimal.TEN)));
     *         }
     *         for (Future<?> future : futures) {
     *             future.get(30, TimeUnit.SECONDS);                  // дочекатися; виняток у потоці стане винятком тут
     *         }
     *     } finally {
     *         pool.shutdownNow();
     *     }
     *
     *     assertMoney("500", TestData.balance(EMF, "UA01"));
     *     assertMoney("500", TestData.balance(EMF, "UA02"));
     *
     * Експеримент: у TransferService.lock(...) прибери LockModeType.PESSIMISTIC_WRITE → тест має впасти
     * (lost update, модуль 9). Поверни блокування.
     *
     * Питання: чому такий тест на H2 нічого б не довів про production?
     */
    @Test
    @DisplayName("6 · 50 паралельних переказів не губляться")
    void concurrentTransfersKeepBalanceCorrect() {
        // TODO 6 Паралельні перекази через ExecutorService (код у коментарі вище)
        fail("TODO 6: напиши тест — див. коментар над методом");
    }

    /** Скільки всього записів Transaction у базі. */
    private long transactionCount() {
        return Tx.read(EMF, em -> em.createQuery("select count(t) from Transaction t", Long.class).getSingleResult());
    }

    /** Порівняння грошей через compareTo: 100 і 100.0000 — однакові суми, хоча equals повернув би false. */
    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Очікували " + expected + ", а маємо " + actual.toPlainString());
    }
}
