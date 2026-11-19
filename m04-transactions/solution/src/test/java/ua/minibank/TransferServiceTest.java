package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Модуль 4 · TransferService")
class TransferServiceTest {

    private static EntityManagerFactory emf;
    private TransferService service;
    private Long uah1;
    private Long uah2;
    private Long usd;

    @BeforeAll
    static void start() {
        emf = Persistence.createEntityManagerFactory("minibank");
    }

    @AfterAll
    static void stop() {
        emf.close();
    }

    @BeforeEach
    void prepare() {
        service = new TransferService(emf);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("truncate table transactions, accounts").executeUpdate();
            Account a = new Account("UA001", CurrencyCode.UAH, new BigDecimal("1000"));
            Account b = new Account("UA002", CurrencyCode.UAH, new BigDecimal("100"));
            Account c = new Account("UA003", CurrencyCode.USD, new BigDecimal("50"));
            em.persist(a);
            em.persist(b);
            em.persist(c);
            em.getTransaction().commit();
            uah1 = a.getId();
            uah2 = b.getId();
            usd = c.getId();
        }
    }

    // ---------- Обов'язкова частина ----------

    @Test
    @DisplayName("1 · переказ змінює обидва баланси і пише 2 операції")
    void transferMovesMoneyAndWritesTwoTransactions() {
        service.transfer(uah1, uah2, new BigDecimal("250"));

        assertBalance(uah1, "750");
        assertBalance(uah2, "350");
        assertEquals(2, transactionCount());
    }

    @Test
    @DisplayName("1 · збій посередині — відкат усього (rollback)")
    void failureAfterDebitRollsBackEverything() {
        TransferService failing = new TransferService(emf, () -> {
            throw new IllegalStateException("Збій посередині переказу");
        });

        assertThrows(IllegalStateException.class, () -> failing.transfer(uah1, uah2, new BigDecimal("250")));

        assertBalance(uah1, "1000");
        assertBalance(uah2, "100");
        assertEquals(0, transactionCount(), "Запис про списання теж має відкотитись: rollback у catch, якщо tx.isActive()");
    }

    @Test
    @DisplayName("1 · неіснуючий рахунок → AccountNotFoundException")
    void unknownAccountIsReported() {
        assertThrows(AccountNotFoundException.class, () -> service.transfer(uah1, 999_999L, BigDecimal.TEN));
    }

    @Test
    @DisplayName("1 · сума ≤ 0 → IllegalArgumentException")
    void nonPositiveAmountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.transfer(uah1, uah2, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(uah1, uah2, new BigDecimal("-5")));
    }

    // ---------- Домашнє завдання ----------

    @Test
    @DisplayName("2 (ДЗ) · недостатньо коштів → InsufficientFundsException, баланси без змін")
    void insufficientFundsLeavesBalancesUntouched() {
        assertThrows(InsufficientFundsException.class, () -> service.transfer(uah2, uah1, new BigDecimal("100.01")));

        assertBalance(uah1, "1000");
        assertBalance(uah2, "100");
        assertEquals(0, transactionCount());
    }

    @Test
    @DisplayName("2 (ДЗ) · різні валюти → CurrencyMismatchException")
    void transferBetweenDifferentCurrenciesIsRejected() {
        assertThrows(CurrencyMismatchException.class, () -> service.transfer(uah1, usd, BigDecimal.TEN));

        assertBalance(uah1, "1000");
        assertBalance(usd, "50");
    }

    private void assertBalance(Long accountId, String expected) {
        try (EntityManager em = emf.createEntityManager()) {
            BigDecimal actual = em.find(Account.class, accountId).getBalance();
            assertEquals(0, new BigDecimal(expected).compareTo(actual),
                    "Очікували " + expected + ", а маємо " + actual.toPlainString());
        }
    }

    private long transactionCount() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select count(t) from Transaction t", Long.class).getSingleResult();
        }
    }
}
