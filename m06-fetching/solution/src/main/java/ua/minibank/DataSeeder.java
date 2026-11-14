package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Тестові дані: N клієнтів, у кожного рахунки UAH і USD, на кожному рахунку 5 операцій.
 */
public final class DataSeeder {

    public static final int ACCOUNTS_PER_CUSTOMER = 2;
    public static final int TRANSACTIONS_PER_ACCOUNT = 5;

    private static final Instant START = Instant.parse("2026-01-01T09:00:00Z");

    private DataSeeder() {
    }

    public static void seed(EntityManagerFactory emf, int customers) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("truncate table transactions, accounts, customers").executeUpdate();

            for (int i = 1; i <= customers; i++) {
                Customer customer = new Customer("Client" + i, "Surname" + i, "client" + i + "@example.com");
                Account uah = new Account(String.format("UA%04d1", i), CurrencyCode.UAH, new BigDecimal("10000"));
                Account usd = new Account(String.format("UA%04d2", i), CurrencyCode.USD, new BigDecimal("500"));
                customer.addAccount(uah);
                customer.addAccount(usd);
                em.persist(customer);

                for (Account account : customer.getAccounts()) {
                    for (int t = 0; t < TRANSACTIONS_PER_ACCOUNT; t++) {
                        TransactionType type = t % 2 == 0 ? TransactionType.CREDIT : TransactionType.DEBIT;
                        Instant createdAt = START.plus((long) i * 24 + t, ChronoUnit.HOURS);
                        em.persist(new Transaction(account, type, new BigDecimal(100 + t * 10), "Операція " + t, createdAt));
                    }
                }
            }
            em.getTransaction().commit();
        }
    }
}
