package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import ua.minibank.domain.Account;
import ua.minibank.domain.CurrencyCode;
import ua.minibank.domain.Customer;
import ua.minibank.domain.Transaction;
import ua.minibank.domain.TransactionType;
import ua.minibank.service.Tx;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Резерв: builder тестових даних. Тест описує лише те, що для нього важливо,
 * решта полів отримує розумні значення за замовчуванням.
 *
 *   Long id = TestData.customer().account("UA01", "1000").account("UA02", CurrencyCode.USD, "50").save(EMF);
 */
public final class TestData {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private TestData() {
    }

    public static CustomerBuilder customer() {
        return new CustomerBuilder();
    }

    public static final class CustomerBuilder {

        private String firstName = "Olena";
        private String lastName = "Koval";
        private final List<AccountSpec> accounts = new ArrayList<>();

        private record AccountSpec(String number, CurrencyCode currency, BigDecimal balance, int transactions) {
        }

        public CustomerBuilder named(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
            return this;
        }

        public CustomerBuilder account(String number, String balance) {
            return account(number, CurrencyCode.UAH, balance);
        }

        public CustomerBuilder account(String number, CurrencyCode currency, String balance) {
            accounts.add(new AccountSpec(number, currency, new BigDecimal(balance), 0));
            return this;
        }

        /** Рахунок з n операціями по 100. */
        public CustomerBuilder accountWithTransactions(String number, int n) {
            accounts.add(new AccountSpec(number, CurrencyCode.UAH, new BigDecimal("1000"), n));
            return this;
        }

        /** Зберігає клієнта, рахунки й операції. Повертає id клієнта. */
        public Long save(EntityManagerFactory emf) {
            return Tx.inTransaction(emf, em -> {
                String email = firstName.toLowerCase() + SEQ.incrementAndGet() + "@example.com";
                Customer customer = new Customer(firstName, lastName, email);
                em.persist(customer);
                for (AccountSpec spec : accounts) {
                    Account account = new Account(customer, spec.number(), spec.currency(), spec.balance());
                    em.persist(account);
                    for (int i = 0; i < spec.transactions(); i++) {
                        em.persist(new Transaction(account, TransactionType.CREDIT, new BigDecimal("100"), "Операція " + i));
                    }
                }
                return customer.getId();
            });
        }
    }

    /** id рахунку за номером. */
    public static Long accountId(EntityManagerFactory emf, String number) {
        return Tx.read(emf, em -> em.createQuery("select a.id from Account a where a.number = :n", Long.class)
                .setParameter("n", number)
                .getSingleResult());
    }

    public static BigDecimal balance(EntityManagerFactory emf, String number) {
        return Tx.read(emf, em -> em.createQuery("select a.balance from Account a where a.number = :n", BigDecimal.class)
                .setParameter("n", number)
                .getSingleResult());
    }
}
