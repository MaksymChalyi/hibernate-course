package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank")) {
            Account olena = new Account("UA001", CurrencyCode.UAH, new BigDecimal("10000"));
            Account ivan = new Account("UA002", CurrencyCode.UAH, new BigDecimal("500"));
            try (EntityManager em = emf.createEntityManager()) {
                em.getTransaction().begin();
                em.persist(olena);
                em.persist(ivan);
                em.getTransaction().commit();
            }

            TransferService service = new TransferService(emf);
            service.transfer(olena.getId(), ivan.getId(), new BigDecimal("2500"));
            System.out.println("Переказ 2500 виконано");

            try {
                service.transfer(ivan.getId(), olena.getId(), new BigDecimal("1000000"));
            } catch (InsufficientFundsException e) {
                System.out.println("Очікувана помилка: " + e.getMessage());
            }

            try (EntityManager em = emf.createEntityManager()) {
                System.out.println(em.find(Account.class, olena.getId()));
                System.out.println(em.find(Account.class, ivan.getId()));
            }
        }
    }
}
