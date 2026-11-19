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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Модуль 9 · Блокування")
class ConcurrencyTest {

    private static EntityManagerFactory emf;
    private Long accountA;
    private Long accountB;

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
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("truncate table transactions, accounts, legacy_accounts").executeUpdate();
            Account a = new Account("UA-A", CurrencyCode.UAH, new BigDecimal("1000"));
            Account b = new Account("UA-B", CurrencyCode.UAH, new BigDecimal("1000"));
            em.persist(a);
            em.persist(b);
            em.getTransaction().commit();
            accountA = a.getId();
            accountB = b.getId();
        }
    }

    // ---------- Обов'язкова частина ----------

    @Test
    @DisplayName("Демо · lost update без захисту: 10 списань → зникло 9")
    void lostUpdateWithoutProtection() throws Exception {
        Long legacyId;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            LegacyAccount legacy = new LegacyAccount(new BigDecimal("1000"));
            em.persist(legacy);
            em.getTransaction().commit();
            legacyId = legacy.getId();
        }

        int threads = 10;
        CountDownLatch allRead = new CountDownLatch(threads);
        NaiveWithdrawService naive = new NaiveWithdrawService(emf);
        runConcurrently(threads, threads, () -> naive.withdraw(legacyId, BigDecimal.TEN, () -> {
            allRead.countDown();
            await(allRead); // кожен потік чекає, доки ВСІ прочитають баланс 1000
        }));

        BigDecimal balance;
        try (EntityManager em = emf.createEntityManager()) {
            balance = em.find(LegacyAccount.class, legacyId).getBalance();
        }
        // Очікували б 900, але кожен потік записав 1000 - 10 = 990: дев'ять списань зникли
        assertMoney("990", balance);
    }

    @Test
    @DisplayName("1 · конфлікт версій виявляється (@Version)")
    void versionConflictIsDetected() {
        try (EntityManager em1 = emf.createEntityManager(); EntityManager em2 = emf.createEntityManager()) {
            em1.getTransaction().begin();
            em2.getTransaction().begin();
            Account first = em1.find(Account.class, accountA);
            Account second = em2.find(Account.class, accountA);

            first.debit(new BigDecimal("100"));
            em1.getTransaction().commit(); // version 0 → 1

            second.debit(new BigDecimal("200"));
            RuntimeException error = assertThrows(RuntimeException.class, () -> em2.getTransaction().commit());
            assertTrue(TransferSupport.isOptimisticLockFailure(error),
                    "Очікували конфлікт версій, а отримали " + error);
        }
        assertMoney("900", balance(accountA)); // друге списання не перезаписало перше
    }

    @Test
    @DisplayName("2 · pessimistic: 100 паралельних переказів — баланс коректний")
    void pessimisticTransfersAreConsistent() throws Exception {
        PessimisticTransferService service = new PessimisticTransferService(emf);

        int failures = runConcurrently(10, 100, () -> service.transfer(accountA, accountB, BigDecimal.TEN));

        assertEquals(0, failures);
        assertMoney("0", balance(accountA));
        assertMoney("2000", balance(accountB));
        assertEquals(200L, transactionCount());
    }

    // ---------- Резерв ----------

    @Test
    @DisplayName("3 (резерв) · зустрічні перекази без deadlock")
    void oppositeTransfersDoNotDeadlock() throws Exception {
        PessimisticTransferService service = new PessimisticTransferService(emf);
        AtomicInteger counter = new AtomicInteger();

        int failures = runConcurrently(10, 100, () -> {
            if (counter.getAndIncrement() % 2 == 0) {
                service.transfer(accountA, accountB, BigDecimal.TEN);
            } else {
                service.transfer(accountB, accountA, BigDecimal.TEN);
            }
        });

        assertEquals(0, failures, "Deadlock: блокуй рахунки в порядку зростання id (завдання 3)");
        assertMoney("1000", balance(accountA));
        assertMoney("1000", balance(accountB));
    }

    // ---------- Домашнє завдання ----------

    @Test
    @DisplayName("4 (ДЗ) · optimistic з retry: жоден переказ не падає")
    void optimisticTransfersWithRetrySucceed() throws Exception {
        OptimisticTransferService service = new OptimisticTransferService(emf, 30);

        int failures = runConcurrently(5, 25, () -> service.transfer(accountA, accountB, BigDecimal.TEN));

        assertEquals(0, failures, "З retry жоден переказ не має впасти");
        assertMoney("750", balance(accountA));
        assertMoney("1250", balance(accountB));
    }

    // ---------- helpers ----------

    /** Запускає tasks копій task у пулі з threads потоків. Повертає кількість задач, що впали. */
    private static int runConcurrently(int threads, int tasks, Runnable task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                futures.add(pool.submit(task));
            }
            int failures = 0;
            for (Future<?> future : futures) {
                try {
                    future.get(60, TimeUnit.SECONDS);
                } catch (java.util.concurrent.ExecutionException e) {
                    failures++;
                    System.out.println("Задача впала: " + e.getCause());
                }
            }
            return failures;
        } finally {
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Потоки не зустрілися");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private BigDecimal balance(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Account.class, id).getBalance();
        }
    }

    private long transactionCount() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select count(t) from Transaction t", Long.class).getSingleResult();
        }
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Очікували " + expected + ", а маємо " + actual.toPlainString());
    }
}
