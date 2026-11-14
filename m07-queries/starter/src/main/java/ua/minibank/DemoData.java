package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Фіксовані дані, на яких тести перевіряють звіти.
 *
 * Клієнти і рахунки:
 *   Olena Koval       — UA01 (UAH, 10000), UA02 (USD, 500)
 *   Ivan Petrenko     — UA03 (UAH, 3000)
 *   Maria Shevchenko  — UA04 (EUR, 800)
 *
 * Час операцій задано за Києвом. Зверни увагу на дві операції UA01 біля межі місяців:
 *   31.01 23:30 — ще січень і за Києвом, і за UTC;
 *   01.02 00:30 — лютий за Києвом, але 31.01 22:30 за UTC.
 */
public final class DemoData {

    public static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private DemoData() {
    }

    public static Instant kyiv(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(KYIV).toInstant();
    }

    public static void seed(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("truncate table transactions, accounts, customers").executeUpdate();

            Customer olena = new Customer("Olena", "Koval", "olena@example.com");
            Customer ivan = new Customer("Ivan", "Petrenko", "ivan@example.com");
            Customer maria = new Customer("Maria", "Shevchenko", "maria@example.com");

            Account ua01 = new Account("UA01", CurrencyCode.UAH, new BigDecimal("10000"));
            Account ua02 = new Account("UA02", CurrencyCode.USD, new BigDecimal("500"));
            Account ua03 = new Account("UA03", CurrencyCode.UAH, new BigDecimal("3000"));
            Account ua04 = new Account("UA04", CurrencyCode.EUR, new BigDecimal("800"));
            olena.addAccount(ua01);
            olena.addAccount(ua02);
            ivan.addAccount(ua03);
            maria.addAccount(ua04);
            em.persist(olena);
            em.persist(ivan);
            em.persist(maria);

            tx(em, ua01, "2026-01-05T10:00", TransactionType.CREDIT, "20000", "Зарплата");
            tx(em, ua01, "2026-01-10T12:00", TransactionType.DEBIT, "1500", "Супермаркет");
            tx(em, ua01, "2026-01-20T09:00", TransactionType.DEBIT, "3500", "Оренда");
            tx(em, ua01, "2026-01-31T23:30", TransactionType.DEBIT, "1000", "Ресторан");
            tx(em, ua01, "2026-02-01T00:30", TransactionType.DEBIT, "700", "Таксі");
            tx(em, ua01, "2026-02-05T10:00", TransactionType.CREDIT, "20000", "Зарплата");
            tx(em, ua01, "2026-02-14T18:00", TransactionType.DEBIT, "2500", "Подарунок");

            tx(em, ua02, "2026-01-15T11:00", TransactionType.CREDIT, "500", "Фриланс");
            tx(em, ua02, "2026-02-10T08:00", TransactionType.DEBIT, "120", "Підписка");

            tx(em, ua03, "2026-01-07T10:00", TransactionType.CREDIT, "15000", "Зарплата");
            tx(em, ua03, "2026-01-12T19:00", TransactionType.DEBIT, "4000", "Техніка");
            tx(em, ua03, "2026-02-03T13:00", TransactionType.DEBIT, "6000", "Ремонт");

            tx(em, ua04, "2026-01-25T15:00", TransactionType.DEBIT, "200", "Квитки");
            tx(em, ua04, "2026-02-20T16:00", TransactionType.DEBIT, "300", "Готель");

            em.getTransaction().commit();
        }
    }

    private static void tx(EntityManager em, Account account, String kyivTime, TransactionType type,
                           String amount, String description) {
        em.persist(new Transaction(account, type, new BigDecimal(amount), description, kyiv(kyivTime)));
    }
}
