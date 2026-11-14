package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class Main {

    public static void main(String[] args) {
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank")) {
            Long customerId;
            try (EntityManager em = emf.createEntityManager()) {
                em.getTransaction().begin();

                Customer olena = new Customer("Olena", "Koval", "olena@example.com");
                Account uah = new Account("UA001", CurrencyCode.UAH, new BigDecimal("15000"));
                Account usd = new Account("UA002", CurrencyCode.USD, new BigDecimal("700"));
                olena.addAccount(uah);
                olena.addAccount(usd);

                // Один persist — три INSERT завдяки cascade PERSIST
                em.persist(olena);

                em.persist(new Transaction(uah, TransactionType.CREDIT, new BigDecimal("20000"), "Зарплата", Instant.now()));
                em.persist(new Transaction(uah, TransactionType.DEBIT, new BigDecimal("5000"), "Оренда", Instant.now()));
                // TODO (домашнє завдання): розкоментуй, коли Card стане entity
                // em.persist(new Card(uah, "4441 **** **** 1234", LocalDate.of(2029, 12, 31)));

                em.getTransaction().commit();
                customerId = olena.getId();
            }

            try (EntityManager em = emf.createEntityManager()) {
                Customer olena = em.find(Customer.class, customerId);
                System.out.println("Клієнт: " + olena);
                // Тут у лозі з'явиться окремий SELECT по accounts: колекція LAZY
                for (Account account : olena.getAccounts()) {
                    System.out.println("  рахунок: " + account);
                }
            }
        }
    }
}
