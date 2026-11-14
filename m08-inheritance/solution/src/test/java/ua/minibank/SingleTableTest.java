package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.minibank.common.CurrencyCode;
import ua.minibank.common.PaymentStatus;
import ua.minibank.singletable.CardPayment;
import ua.minibank.singletable.Payment;
import ua.minibank.singletable.SepaPayment;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SingleTableTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void start() {
        emf = Persistence.createEntityManagerFactory("single-table");
    }

    @AfterAll
    static void stop() {
        emf.close();
    }

    @BeforeEach
    void seed() {
        Db.run(emf, em -> {
            em.createNativeQuery("truncate table payments").executeUpdate();
            em.persist(new CardPayment(new BigDecimal("450"), CurrencyCode.UAH, "UA01", "4441 **** **** 1234", "Silpo"));
            em.persist(new SepaPayment(new BigDecimal("1200"), CurrencyCode.EUR, "UA04", "DE89370400440532013000", "Hans Muller"));
        });
    }

    @Test
    void wholeHierarchyLivesInOneTable() {
        assertEquals(List.of("payments"), Db.tables(emf, "m08_single"));
    }

    @Test
    void discriminatorColumnStoresType() {
        List<?> types = Db.get(emf, em -> em.createNativeQuery("select payment_type from payments order by id").getResultList());

        assertEquals(List.of("CARD", "SEPA"), types);
    }

    @Test
    void cardRowHasNullSepaColumns() {
        Object iban = Db.get(emf, em -> em.createNativeQuery(
                "select beneficiary_iban from payments where payment_type = 'CARD'").getSingleResult());

        assertNull(iban, "Ціна SINGLE_TABLE: чужі колонки порожні");
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
    void typeFunctionFiltersBySubclass() {
        List<Payment> cards = Db.get(emf, em ->
                em.createQuery("select p from Payment p where type(p) = CardPayment", Payment.class).getResultList());

        assertEquals(1, cards.size());
    }

    @Test
    void baseEntityFillsTimestamps() {
        Long id = Db.get(emf, em -> em.createQuery("select p.id from Payment p where type(p) = CardPayment", Long.class)
                .getSingleResult());

        Db.run(emf, em -> em.find(Payment.class, id).complete());

        Payment payment = Db.get(emf, em -> em.find(Payment.class, id));
        assertNotNull(payment.getCreatedAt());
        assertFalse(payment.getUpdatedAt().isBefore(payment.getCreatedAt()));
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    }
}
