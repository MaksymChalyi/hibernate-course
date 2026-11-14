package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Усі методи нижче працюють правильно, але повільно. Тести FetchingTest рахують SQL-запити.
 * Завдання — не змінити результат, а зменшити кількість запитів.
 */
public class AccountReportService {

    private final EntityManagerFactory emf;

    public AccountReportService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     * TODO 2. Зараз: 1 запит на рахунки + 1 запит на кожного клієнта (N+1).
     * Перепиши запит так, щоб клієнти прийшли тим самим SELECT (JOIN FETCH).
     */
    public List<String> accountOwnerLines() {
        try (EntityManager em = emf.createEntityManager()) {
            List<Account> accounts = em.createQuery("select a from Account a order by a.id", Account.class)
                    .getResultList();
            return accounts.stream()
                    .map(a -> a.getNumber() + " — " + a.getCustomer().getFullName())
                    .toList();
        }
    }

    /**
     * TODO 3. Зараз: клієнт + його рахунки + окремий запит операцій на кожен рахунок.
     * Зроби один запит: від Transaction через account до customer, з JOIN FETCH рахунку.
     * Сортування: createdAt desc, потім id desc.
     */
    public List<TransactionView> transactionHistory(Long customerId) {
        try (EntityManager em = emf.createEntityManager()) {
            Customer customer = em.find(Customer.class, customerId);
            List<TransactionView> result = new ArrayList<>();
            for (Account account : customer.getAccounts()) {
                List<Transaction> transactions = em.createQuery(
                                "select t from Transaction t where t.account = :account", Transaction.class)
                        .setParameter("account", account)
                        .getResultList();
                for (Transaction t : transactions) {
                    result.add(new TransactionView(account.getNumber(), t.getType(), t.getAmount(), t.getCreatedAt()));
                }
            }
            result.sort(Comparator.comparing(TransactionView::createdAt).reversed());
            return result;
        }
    }

    /**
     * TODO 4. Тест customersWithAccountsAreUsableAfterEntityManagerIsClosed падає з
     * LazyInitializationException: рахунки не завантажені, а EntityManager уже закритий.
     * Виправ запит так, щоб рахунки прийшли разом із клієнтами одним SELECT.
     * Не роби колекцію EAGER і не тримай EntityManager відкритим — це не рішення.
     */
    public List<Customer> customersWithAccounts() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select c from Customer c order by c.id", Customer.class)
                    .getResultList();
        }
    }

    /**
     * TODO 5 (резерв). Те саме, що accountOwnerLines, але через entity graph:
     * em.createEntityGraph(Account.class), addAttributeNodes("customer"),
     * підказка запиту "jakarta.persistence.fetchgraph".
     */
    public List<String> accountOwnerLinesWithEntityGraph() {
        throw new UnsupportedOperationException("TODO: accountOwnerLinesWithEntityGraph");
    }

    /**
     * TODO 6 (резерв). DTO-проєкція: "select new ua.minibank.AccountOwnerView(...) from Account a join a.customer c".
     */
    public List<AccountOwnerView> accountOwnerViews() {
        throw new UnsupportedOperationException("TODO: accountOwnerViews");
    }
}
