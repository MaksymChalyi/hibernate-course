package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.minibank.common.CurrencyCode;
import ua.minibank.tableperclass.CardPayment;
import ua.minibank.tableperclass.Payment;
import ua.minibank.tableperclass.SepaPayment;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Резерв: TABLE_PER_CLASS. */
@DisplayName("Модуль 8 · TABLE_PER_CLASS (резерв, завдання 8–10)")
class TablePerClassTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void start() {
        emf = Persistence.createEntityManagerFactory("table-per-class");
    }

    @AfterAll
    static void stop() {
        emf.close();
    }

    @BeforeEach
    void seed() {
        Db.run(emf, em -> {
            em.createNativeQuery("truncate table card_payments, sepa_payments").executeUpdate();
            em.persist(new CardPayment(new BigDecimal("450"), CurrencyCode.UAH, "UA01", "4441 **** **** 1234", "Silpo"));
            em.persist(new SepaPayment(new BigDecimal("1200"), CurrencyCode.EUR, "UA04", "DE89370400440532013000", "Hans Muller"));
        });
    }

    @Test
    @DisplayName("8–10 · таблиці лише в конкретних класів")
    void onlyConcreteClassesHaveTables() {
        assertEquals(List.of("card_payments", "sepa_payments"), Db.tables(emf, "m08_tpc"));
    }

    @Test
    @DisplayName("8–10 · поліморфний запит — UNION ALL, id унікальні")
    void polymorphicQueryUnionsAllTables() {
        // У лозі буде UNION ALL двох таблиць
        List<Payment> payments = Db.get(emf, em ->
                em.createQuery("select p from Payment p order by p.id", Payment.class).getResultList());

        assertEquals(2, payments.size());
        assertNotEquals(payments.get(0).getId(), payments.get(1).getId(), "id унікальні в усій ієрархії");
    }
}
