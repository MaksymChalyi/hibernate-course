package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelationshipsTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void start() {
        emf = Persistence.createEntityManagerFactory("minibank");
    }

    @AfterAll
    static void stop() {
        emf.close();
    }

    @BeforeEach
    void clean() {
        run(em -> {
            List<?> tables = em.createNativeQuery(
                    "select cast(table_name as text) from information_schema.tables where table_schema = 'm05'").getResultList();
            String names = String.join(", ", tables.stream().map(Object::toString).toList());
            em.createNativeQuery("truncate table " + names + " cascade").executeUpdate();
        });
    }

    // ---------- Обов'язкова частина ----------

    @Test
    void addAccountSetsBothSides() {
        Customer olena = new Customer("Olena", "Koval", "olena@example.com");
        Account account = new Account("UA001", CurrencyCode.UAH, BigDecimal.ZERO);

        olena.addAccount(account);

        assertEquals(1, olena.getAccounts().size());
        assertSame(olena, account.getCustomer(), "Owning side теж має знати про клієнта");
    }

    @Test
    void persistingCustomerCascadesToAccounts() {
        Long customerId = persistCustomerWithTwoAccounts();

        Long rows = get(em -> em.createQuery(
                        "select count(a) from Account a where a.customer.id = :id", Long.class)
                .setParameter("id", customerId)
                .getSingleResult());
        assertEquals(2L, rows, "Один persist(customer) має зберегти і рахунки");
    }

    @Test
    void customerSeesItsAccountsAfterReload() {
        Long customerId = persistCustomerWithTwoAccounts();

        int count = get(em -> em.find(Customer.class, customerId).getAccounts().size());

        assertEquals(2, count);
    }

    @Test
    void accountKnowsItsOwner() {
        persistCustomerWithTwoAccounts();

        String owner = get(em -> em.createQuery("select a from Account a where a.number = 'UA001'", Account.class)
                .getSingleResult()
                .getCustomer()
                .getFirstName());

        assertEquals("Olena", owner);
    }

    @Test
    void foreignKeyLivesInAccountsTableAndNoJoinTableIsCreated() {
        List<?> tables = get(em -> em.createNativeQuery(
                "select cast(table_name as text) from information_schema.tables where table_schema = 'm05'").getResultList());
        assertFalse(tables.contains("customers_accounts"), "Зайва таблиця: перевір mappedBy");

        Number fkColumns = get(em -> (Number) em.createNativeQuery("""
                select count(*) from information_schema.columns
                where table_schema = 'm05' and table_name = 'accounts' and column_name = 'customer_id'
                """).getSingleResult());
        assertEquals(1, fkColumns.intValue());
    }

    @Test
    void transactionsReferenceTheirAccount() {
        persistCustomerWithTwoAccounts();
        run(em -> {
            Account uah = em.createQuery("select a from Account a where a.number = 'UA001'", Account.class)
                    .getSingleResult();
            em.persist(new Transaction(uah, TransactionType.CREDIT, new BigDecimal("1000"), "Зарплата", Instant.now()));
            em.persist(new Transaction(uah, TransactionType.DEBIT, new BigDecimal("300"), "Кафе", Instant.now()));
        });

        Long count = get(em -> em.createQuery(
                "select count(t) from Transaction t where t.account.number = 'UA001'", Long.class).getSingleResult());
        assertEquals(2L, count);
    }

    @Test
    void removingAccountDoesNotRemoveCustomer() {
        Long customerId = persistCustomerWithTwoAccounts();

        run(em -> em.remove(em.createQuery("select a from Account a where a.number = 'UA002'", Account.class)
                .getSingleResult()));

        assertNotNull(get(em -> em.find(Customer.class, customerId)), "cascade REMOVE на @ManyToOne — помилка");
    }

    // ---------- Домашнє завдання: Card ----------

    @Test
    void cardBelongsToAccount() {
        persistCustomerWithTwoAccounts();
        Long[] cardId = new Long[1];
        run(em -> {
            Account uah = em.createQuery("select a from Account a where a.number = 'UA001'", Account.class)
                    .getSingleResult();
            Card card = new Card(uah, "4441 **** **** 1234", LocalDate.of(2029, 12, 31));
            em.persist(card);
            cardId[0] = card.getId();
        });

        String accountNumber = get(em -> em.find(Card.class, cardId[0]).getAccount().getNumber());
        assertEquals("UA001", accountNumber);
    }

    @Test
    void secondCardForTheSameAccountIsRejected() {
        persistCustomerWithTwoAccounts();
        Consumer<EntityManager> addCard = em -> {
            Account uah = em.createQuery("select a from Account a where a.number = 'UA001'", Account.class)
                    .getSingleResult();
            em.persist(new Card(uah, "4441 **** **** " + System.nanoTime() % 10000, LocalDate.of(2029, 12, 31)));
        };
        run(addCard);

        assertThrows(PersistenceException.class, () -> run(addCard), "account_id у cards має бути unique");
    }

    // ---------- helpers ----------

    private Long persistCustomerWithTwoAccounts() {
        Customer olena = new Customer("Olena", "Koval", "olena@example.com");
        olena.addAccount(new Account("UA001", CurrencyCode.UAH, new BigDecimal("1000")));
        olena.addAccount(new Account("UA002", CurrencyCode.USD, new BigDecimal("50")));
        run(em -> em.persist(olena));
        return olena.getId();
    }

    private static void run(Consumer<EntityManager> work) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                work.accept(em);
                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    private static <T> T get(Function<EntityManager, T> work) {
        try (EntityManager em = emf.createEntityManager()) {
            return work.apply(em);
        }
    }
}
