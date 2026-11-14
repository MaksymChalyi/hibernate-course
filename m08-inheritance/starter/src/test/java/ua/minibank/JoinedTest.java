package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.minibank.common.CurrencyCode;
import ua.minibank.joined.CardPayment;
import ua.minibank.joined.Payment;
import ua.minibank.joined.SepaPayment;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JoinedTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void start() {
        emf = Persistence.createEntityManagerFactory("joined");
    }

    @AfterAll
    static void stop() {
        emf.close();
    }

    @BeforeEach
    void seed() {
        Db.run(emf, em -> {
            em.createNativeQuery("truncate table card_payments, sepa_payments, payments").executeUpdate();
            em.persist(new CardPayment(new BigDecimal("450"), CurrencyCode.UAH, "UA01", "4441 **** **** 1234", "Silpo"));
            em.persist(new SepaPayment(new BigDecimal("1200"), CurrencyCode.EUR, "UA04", "DE89370400440532013000", "Hans Muller"));
        });
    }

    @Test
    void parentAndChildTablesExist() {
        assertEquals(List.of("card_payments", "payments", "sepa_payments"), Db.tables(emf, "m08_joined"));
    }

    @Test
    void commonFieldsInParentSpecificFieldsInChild() {
        assertEquals(2, Db.count(emf, "payments"));
        assertEquals(1, Db.count(emf, "card_payments"));
        assertEquals(1, Db.count(emf, "sepa_payments"));
    }

    @Test
    void childRowSharesIdWithParentRow() {
        Number matched = Db.get(emf, em -> (Number) em.createNativeQuery(
                "select count(*) from payments p join card_payments c on c.payment_id = p.id").getSingleResult());

        assertEquals(1, matched.intValue());
    }

    @Test
    void polymorphicQueryReturnsConcreteTypes() {
        List<Payment> payments = Db.get(emf, em ->
                em.createQuery("select p from Payment p order by p.id", Payment.class).getResultList());

        assertEquals(2, payments.size());
        assertInstanceOf(CardPayment.class, payments.get(0));
        assertInstanceOf(SepaPayment.class, payments.get(1));
    }

    @Test
    void querySubclassDirectly() {
        List<CardPayment> cards = Db.get(emf, em ->
                em.createQuery("select c from CardPayment c where c.merchant = 'Silpo'", CardPayment.class).getResultList());

        assertEquals(1, cards.size());
        assertEquals("UA01", cards.get(0).getPayerAccount());
    }
}
