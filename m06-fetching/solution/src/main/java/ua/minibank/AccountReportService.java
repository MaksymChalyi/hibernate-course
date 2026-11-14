package ua.minibank;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class AccountReportService {

    private final EntityManagerFactory emf;

    public AccountReportService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     * Рядки "номер рахунку — ім'я власника".
     * JOIN FETCH завантажує рахунки разом із клієнтами одним SELECT.
     * Без нього: 1 запит на рахунки + по запиту на кожного клієнта (N+1).
     */
    public List<String> accountOwnerLines() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Account> accounts = em.createQuery(
                    "select a from Account a join fetch a.customer order by a.id", Account.class)
                    .getResultList();
            return accounts.stream()
                    .map(a -> a.getNumber() + " — " + a.getCustomer().getFullName())
                    .toList();
        }
    }

    /**
     * Історія операцій клієнта по всіх рахунках, нові зверху.
     * Один запит: від операцій через рахунок до клієнта, рахунок підтягується JOIN FETCH.
     */
    public List<TransactionView> transactionHistory(Long customerId) {
        try (EntityManager em = emf.createEntityManager()) {
            List<Transaction> transactions = em.createQuery("""
                            select t from Transaction t
                            join fetch t.account a
                            where a.customer.id = :customerId
                            order by t.createdAt desc, t.id desc
                            """, Transaction.class)
                    .setParameter("customerId", customerId)
                    .getResultList();
            return transactions.stream()
                    .map(t -> new TransactionView(t.getAccount().getNumber(), t.getType(), t.getAmount(), t.getCreatedAt()))
                    .toList();
        }
    }

    /**
     * Клієнти з уже завантаженими рахунками: ними можна користуватися після закриття EntityManager.
     * LEFT JOIN FETCH — щоб не втратити клієнтів без рахунків.
     * Hibernate 6 сам прибирає дублікати клієнтів, які дає JOIN з колекцією.
     */
    public List<Customer> customersWithAccounts() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                    "select c from Customer c left join fetch c.accounts order by c.id", Customer.class)
                    .getResultList();
        }
    }

    /** Резерв: те саме, що accountOwnerLines, але через entity graph замість JOIN FETCH у тексті запиту. */
    public List<String> accountOwnerLinesWithEntityGraph() {
        try (EntityManager em = emf.createEntityManager()) {
            EntityGraph<Account> graph = em.createEntityGraph(Account.class);
            graph.addAttributeNodes("customer");
            return em.createQuery("select a from Account a order by a.id", Account.class)
                    .setHint("jakarta.persistence.fetchgraph", graph)
                    .getResultList()
                    .stream()
                    .map(a -> a.getNumber() + " — " + a.getCustomer().getFullName())
                    .toList();
        }
    }

    /** Резерв: DTO-проєкція. Тільки потрібні колонки, жодних entity в Persistence Context. */
    public List<AccountOwnerView> accountOwnerViews() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            select new ua.minibank.AccountOwnerView(a.number, c.firstName, c.lastName)
                            from Account a join a.customer c
                            order by a.id
                            """, AccountOwnerView.class)
                    .getResultList();
        }
    }
}
