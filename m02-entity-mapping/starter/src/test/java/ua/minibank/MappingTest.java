package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тести модуля 2. Номер на початку назви = номер завдання (кроку) у коді.
 * Тести перевіряють і дані, і саму таблицю: які колонки й типи створив Hibernate.
 */
@DisplayName("Модуль 2 · Mapping")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MappingTest {

    private static final Currency UAH = Currency.getInstance("UAH");
    private static EntityManagerFactory emf;

    @BeforeAll
    static void start() {
        emf = Persistence.createEntityManagerFactory("minibank");
    }

    @AfterAll
    static void stop() {
        if (emf != null) {
            emf.close();
        }
    }

    @BeforeEach
    void clean() {
        inTransaction(em -> em.createNativeQuery("truncate table customers, accounts").executeUpdate());
    }

    // ---------- Завдання 1–2: Customer і Address ----------

    @Test
    @Order(1)
    @DisplayName("1.2 · id клієнта береться з послідовності customer_seq з кроком 50")
    void customerIdComesFromSequenceWithAllocationSize50() {
        Number incrementBy = singleValue(
                "select increment_by from pg_sequences where schemaname = 'm02' and sequencename = 'customer_seq'");
        assertEquals(50L, incrementBy.longValue(), "Перевір sequenceName і allocationSize у @SequenceGenerator");
    }

    @Test
    @Order(2)
    @DisplayName("1.3 · first_name і last_name — varchar(100)")
    void nameColumnsHaveLength100() {
        assertEquals(100, columnLength("customers", "first_name"), "Додай length = 100 у @Column над firstName");
        assertEquals(100, columnLength("customers", "last_name"), "Додай length = 100 у @Column над lastName");
    }

    @Test
    @Order(3)
    @DisplayName("1.3 · прізвище обов'язкове (NOT NULL)")
    void requiredFieldIsChecked() {
        Customer withoutLastName = new Customer("Taras", null, "taras@example.com", null);

        assertThrows(PersistenceException.class, () -> inTransaction(em -> em.persist(withoutLastName)),
                "Збереження без прізвища має падати: nullable = false у @Column над lastName");
    }

    @Test
    @Order(4)
    @DisplayName("1.5 · статус зберігається текстом 'ACTIVE', а не числом")
    void statusIsStoredAsText() {
        inTransaction(em -> em.persist(new Customer("Ivan", "Petrenko", "ivan@example.com", null)));

        Object status = singleValue("select cast(status as text) from customers");
        assertEquals("ACTIVE", status, "Потрібен @Enumerated(EnumType.STRING)");
    }

    @Test
    @Order(5)
    @DisplayName("1.6–1.7 · registered_at — timestamp with time zone, birth_date — date")
    void dateColumnsHaveRightTypes() {
        assertEquals("timestamp with time zone", columnType("customers", "registered_at"),
                "Колонка registered_at: перевір @Column(name = \"registered_at\")");
        assertEquals("date", columnType("customers", "birth_date"),
                "Колонка birth_date: перевір @Column(name = \"birth_date\")");
    }

    @Test
    @Order(6)
    @DisplayName("1.8 + 2 · клієнт з адресою зберігається і читається назад")
    void customerWithAddressIsSavedAndLoaded() {
        Customer olena = new Customer("Olena", "Koval", "olena@example.com",
                new Address("Kyiv", "Khreshchatyk 1", "01001"));
        olena.setBirthDate(LocalDate.of(1995, 3, 14));
        inTransaction(em -> em.persist(olena));

        try (EntityManager em = emf.createEntityManager()) {
            Customer loaded = em.find(Customer.class, olena.getId());
            assertNotNull(loaded.getAddress(), "Адреса не завантажилась: @Embedded над полем address і @Embeddable над Address");
            assertEquals("Kyiv", loaded.getAddress().getCity());
            assertEquals(CustomerStatus.ACTIVE, loaded.getStatus());
            assertEquals(LocalDate.of(1995, 3, 14), loaded.getBirthDate());
            assertNotNull(loaded.getRegisteredAt());
        }
    }

    @Test
    @Order(7)
    @DisplayName("2 · колонки адреси лежать у таблиці customers (окремої таблиці немає)")
    void addressColumnsLiveInCustomersTable() {
        inTransaction(em -> em.persist(new Customer("Olena", "Koval", "olena@example.com",
                new Address("Lviv", "Rynok 1", "79000"))));

        assertEquals("Lviv", singleValue("select city from customers"));
        assertEquals("79000", singleValue("select postal_code from customers"),
                "Колонка postal_code: @Column(name = \"postal_code\") над postalCode в Address");
    }

    // ---------- Завдання 3: Account ----------

    @Test
    @Order(8)
    @DisplayName("3.5 · balance — numeric(19,4)")
    void balanceColumnIsNumeric19x4() {
        Number precision = singleValue("""
                select numeric_precision from information_schema.columns
                where table_schema = 'm02' and table_name = 'accounts' and column_name = 'balance'""");
        Number scale = singleValue("""
                select numeric_scale from information_schema.columns
                where table_schema = 'm02' and table_name = 'accounts' and column_name = 'balance'""");
        assertEquals(19, precision.intValue(), "precision = 19 у @Column над balance");
        assertEquals(4, scale.intValue(), "scale = 4 у @Column над balance");
    }

    @Test
    @Order(9)
    @DisplayName("3.5 · баланс зберігає 4 знаки після коми")
    void balanceKeepsFourDigitsAfterPoint() {
        Account account = new Account("UA001", UAH, new BigDecimal("1234.5"));
        inTransaction(em -> em.persist(account));

        assertEquals("1234.5000", singleValue("select cast(balance as text) from accounts"));

        try (EntityManager em = emf.createEntityManager()) {
            Account loaded = em.find(Account.class, account.getId());
            // compareTo, бо new BigDecimal("1234.5").equals(new BigDecimal("1234.5000")) == false
            assertEquals(0, new BigDecimal("1234.5").compareTo(loaded.getBalance()));
        }
    }

    @Test
    @Order(10)
    @DisplayName("3 · чому не double: 0.1 + 0.2 != 0.3")
    void doubleCannotStoreMoneyExactly() {
        assertNotEquals(0.3, 0.1 + 0.2);
        assertEquals(new BigDecimal("0.3"), new BigDecimal("0.1").add(new BigDecimal("0.2")));
    }

    // ---------- Завдання 4 (резерв) і 5 (ДЗ) ----------

    @Test
    @Order(11)
    @DisplayName("4 (резерв) · кредитний ліміт у колонках credit_limit_amount і credit_limit_currency")
    void creditLimitUsesOverriddenColumns() {
        Account account = new Account("UA003", UAH, BigDecimal.ZERO);
        account.setCreditLimit(Money.of("20000", "UAH"));
        inTransaction(em -> em.persist(account));

        Object[] row = singleRow("select cast(credit_limit_amount as text), credit_limit_currency from accounts");
        assertEquals("20000.0000", row[0]);
        assertEquals("UAH", row[1]);

        try (EntityManager em = emf.createEntityManager()) {
            Account loaded = em.find(Account.class, account.getId());
            assertEquals(Money.of("20000", "UAH"), loaded.getCreditLimit());
        }
    }

    @Test
    @Order(12)
    @DisplayName("5 (ДЗ) · валюта зберігається кодом 'USD'")
    void currencyIsStoredAsIsoCode() {
        inTransaction(em -> em.persist(new Account("UA002", Currency.getInstance("USD"), BigDecimal.TEN)));

        assertEquals("USD", singleValue("select currency from accounts"));
    }

    @Test
    @Order(13)
    @DisplayName("5 (ДЗ) · CurrencyConverter перетворює в обидва боки і не падає на null")
    void converterWorksBothWays() {
        CurrencyConverter converter = new CurrencyConverter();

        assertEquals("EUR", converter.convertToDatabaseColumn(Currency.getInstance("EUR")));
        assertEquals(Currency.getInstance("EUR"), converter.convertToEntityAttribute("EUR"));
        assertEquals(null, converter.convertToDatabaseColumn(null));
        assertEquals(null, converter.convertToEntityAttribute(null));
    }

    // ---------- helpers ----------

    private static int columnLength(String table, String column) {
        Number length = singleValue("select character_maximum_length from information_schema.columns "
                + "where table_schema = 'm02' and table_name = '" + table + "' and column_name = '" + column + "'");
        return length.intValue();
    }

    private static String columnType(String table, String column) {
        return singleValue("select cast(data_type as text) from information_schema.columns "
                + "where table_schema = 'm02' and table_name = '" + table + "' and column_name = '" + column + "'");
    }

    @SuppressWarnings("unchecked")
    private static <T> T singleValue(String sql) {
        try (EntityManager em = emf.createEntityManager()) {
            return (T) em.createNativeQuery(sql).getSingleResult();
        }
    }

    private static Object[] singleRow(String sql) {
        return singleValue(sql);
    }

    private static void inTransaction(Consumer<EntityManager> work) {
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
}
