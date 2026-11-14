package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.List;

/**
 * Порівняння: наївний цикл (N+1) і JOIN FETCH. Дивись у лог і на лічильник запитів.
 */
public class Main {

    public static void main(String[] args) {
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank")) {
            DataSeeder.seed(emf, 20);
            Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();

            stats.clear();
            try (EntityManager em = emf.createEntityManager()) {
                List<Account> accounts = em.createQuery("select a from Account a", Account.class).getResultList();
                for (Account a : accounts) {
                    a.getCustomer().getFullName(); // кожен новий клієнт — окремий SELECT
                }
            }
            System.out.println("Наївний цикл: запитів = " + stats.getPrepareStatementCount());

            stats.clear();
            new AccountReportService(emf).accountOwnerLines();
            System.out.println("JOIN FETCH:   запитів = " + stats.getPrepareStatementCount());
        }
    }
}
