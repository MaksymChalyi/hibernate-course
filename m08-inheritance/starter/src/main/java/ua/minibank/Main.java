package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import ua.minibank.common.CurrencyCode;

import java.math.BigDecimal;

/**
 * Запусти і порівняй у лозі SQL для двох стратегій:
 * INSERT, завантаження конкретного типу і поліморфний запит.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("========== SINGLE_TABLE ==========");
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("single-table")) {
            try (EntityManager em = emf.createEntityManager()) {
                em.getTransaction().begin();
                em.persist(new ua.minibank.singletable.CardPayment(new BigDecimal("450"), CurrencyCode.UAH,
                        "UA01", "4441 **** **** 1234", "Silpo"));
                em.persist(new ua.minibank.singletable.SepaPayment(new BigDecimal("1200"), CurrencyCode.EUR,
                        "UA04", "DE89370400440532013000", "Hans Muller"));
                em.getTransaction().commit();
            }
            try (EntityManager em = emf.createEntityManager()) {
                em.createQuery("select p from Payment p", ua.minibank.singletable.Payment.class).getResultList();
            }
        }

        System.out.println("========== JOINED ==========");
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("joined")) {
            try (EntityManager em = emf.createEntityManager()) {
                em.getTransaction().begin();
                em.persist(new ua.minibank.joined.CardPayment(new BigDecimal("450"), CurrencyCode.UAH,
                        "UA01", "4441 **** **** 1234", "Silpo"));
                em.persist(new ua.minibank.joined.SepaPayment(new BigDecimal("1200"), CurrencyCode.EUR,
                        "UA04", "DE89370400440532013000", "Hans Muller"));
                em.getTransaction().commit();
            }
            try (EntityManager em = emf.createEntityManager()) {
                em.createQuery("select p from Payment p", ua.minibank.joined.Payment.class).getResultList();
            }
        }
    }
}
