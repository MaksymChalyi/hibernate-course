package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;

/**
 * Lost update у чистому вигляді: прочитати → порахувати в Java → записати.
 * Два потоки читають 1000, обидва пишуть 990 — одне списання зникає.
 */
public class NaiveWithdrawService {

    private final EntityManagerFactory emf;

    public NaiveWithdrawService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /** afterRead — пауза між читанням і записом, щоб тест надійно зіштовхнув потоки. */
    public void withdraw(Long accountId, BigDecimal amount, Runnable afterRead) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            LegacyAccount account = em.find(LegacyAccount.class, accountId);
            afterRead.run();
            account.setBalance(account.getBalance().subtract(amount));
            em.getTransaction().commit();
        }
    }
}
