package ua.minibank.service;

import jakarta.persistence.EntityManagerFactory;
import ua.minibank.domain.Transaction;

import java.util.List;

public class ReportService {

    private final EntityManagerFactory emf;

    public ReportService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     * Історія операцій клієнта по всіх рахунках. Один запит завдяки JOIN FETCH.
     * Якщо хтось прибере "fetch", кожна операція довантажить свій рахунок окремим SELECT (N+1).
     * Від цього захищає тест ReportServiceTest.historyUsesOneQuery.
     */
    public List<TransactionView> customerHistory(Long customerId) {
        return Tx.read(emf, em -> em.createQuery("""
                        select t from Transaction t
                        join fetch t.account a
                        where a.customer.id = :customerId
                        order by t.createdAt desc, t.id desc
                        """, Transaction.class)
                .setParameter("customerId", customerId)
                .getResultList()
                .stream()
                .map(t -> new TransactionView(t.getAccount().getNumber(), t.getType(), t.getAmount(), t.getCreatedAt()))
                .toList());
    }
}
