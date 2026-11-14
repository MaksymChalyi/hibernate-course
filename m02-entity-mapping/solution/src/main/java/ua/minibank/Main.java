package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

public class Main {

    public static void main(String[] args) {
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank")) {
            Long customerId;
            Long accountId;
            try (EntityManager em = emf.createEntityManager()) {
                em.getTransaction().begin();

                Customer olena = new Customer("Olena", "Koval", "olena@example.com",
                        new Address("Kyiv", "Khreshchatyk 1", "01001"));
                olena.setBirthDate(LocalDate.of(1995, 3, 14));
                em.persist(olena);

                Account account = new Account("UA213223130000026007233566001", Currency.getInstance("UAH"),
                        new BigDecimal("15000.50"));
                account.setCreditLimit(Money.of("20000", "UAH"));
                em.persist(account);

                em.getTransaction().commit();
                customerId = olena.getId();
                accountId = account.getId();
            }

            // Новий EntityManager — читаємо з бази, а не з пам'яті
            try (EntityManager em = emf.createEntityManager()) {
                System.out.println(em.find(Customer.class, customerId));
                System.out.println(em.find(Account.class, accountId));
                System.out.println("Статус у базі: "
                        + em.createNativeQuery("select status from customers").getSingleResult());
                System.out.println("Баланс у базі: "
                        + em.createNativeQuery("select cast(balance as text) from accounts").getSingleResult());
            }
        }
    }
}
