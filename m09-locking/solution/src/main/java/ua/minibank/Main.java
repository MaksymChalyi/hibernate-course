package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 100 паралельних переказів по 10 з рахунку A на B. Порівняй SQL у лозі:
 * pessimistic — "for update"/"for no key update", optimistic — "where ... and version = ?".
 */
public class Main {

    public static void main(String[] args) throws Exception {
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank")) {
            Account a = new Account("UA-A", CurrencyCode.UAH, new BigDecimal("1000"));
            Account b = new Account("UA-B", CurrencyCode.UAH, new BigDecimal("0"));
            try (EntityManager em = emf.createEntityManager()) {
                em.getTransaction().begin();
                em.persist(a);
                em.persist(b);
                em.getTransaction().commit();
            }

            PessimisticTransferService service = new PessimisticTransferService(emf);
            ExecutorService pool = Executors.newFixedThreadPool(10);
            for (int i = 0; i < 100; i++) {
                pool.submit(() -> service.transfer(a.getId(), b.getId(), BigDecimal.TEN));
            }
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.MINUTES);

            try (EntityManager em = emf.createEntityManager()) {
                System.out.println("A = " + em.find(Account.class, a.getId()).getBalance().toPlainString());
                System.out.println("B = " + em.find(Account.class, b.getId()).getBalance().toPlainString());
            }
        }
    }
}
