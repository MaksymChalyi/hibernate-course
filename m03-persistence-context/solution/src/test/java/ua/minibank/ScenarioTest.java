package ua.minibank;

import jakarta.persistence.EntityManager;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Перевіряє прогнози з Predictions: запускає сценарій і рахує SQL-запити.
 */
@DisplayName("Модуль 3 · Прогнози SQL (завдання 1)")
class ScenarioTest {

    private record Case(String name, Consumer<Scenarios.Fixture> scenario, int predicted) {
    }

    @TestFactory
    List<DynamicTest> predictionsMatchReality() {
        List<Case> cases = List.of(
                new Case("1.1 · Сценарій 1: два find в одному EntityManager", Scenarios::s1, Predictions.SCENARIO_1),
                new Case("1.2 · Сценарій 2: find у двох EntityManager", Scenarios::s2, Predictions.SCENARIO_2),
                new Case("1.3 · Сценарій 3: зміна поля без update()", Scenarios::s3, Predictions.SCENARIO_3),
                new Case("1.4 · Сценарій 4: запис того самого значення", Scenarios::s4, Predictions.SCENARIO_4),
                new Case("1.5 · Сценарій 5: зміна + запит + rollback", Scenarios::s5, Predictions.SCENARIO_5),
                new Case("1.6 (ДЗ) · Сценарій 6: clear() між find", Scenarios::s6, Predictions.SCENARIO_6),
                new Case("1.7 (ДЗ) · Сценарій 7: getReference + getId", Scenarios::s7, Predictions.SCENARIO_7),
                new Case("1.8 (ДЗ) · Сценарій 8: find + remove", Scenarios::s8, Predictions.SCENARIO_8),
                new Case("1.9 (ДЗ) · Сценарій 9: запит, потім find", Scenarios::s9, Predictions.SCENARIO_9),
                new Case("1.10 (ДЗ) · Сценарій 10: merge без змін", Scenarios::s10, Predictions.SCENARIO_10)
        );

        return cases.stream()
                .map(c -> dynamicTest(c.name(), () -> {
                    assertNotEquals(-1, c.predicted(), "Спочатку впиши прогноз у Predictions (зараз там -1)");

                    Scenarios.Fixture fixture = prepareFixture();
                    Statistics stats = Jpa.statistics();
                    stats.clear();

                    c.scenario().accept(fixture);

                    assertEquals(c.predicted(), stats.getPrepareStatementCount(),
                            "Прогноз не збігся. У консолі цього тесту видно кожен SQL — порахуй їх і знайди, який рядок сценарію його спричинив");
                }))
                .toList();
    }

    /** Чиста таблиця, один клієнт і його detached-копія. Запити тут не рахуються. */
    private Scenarios.Fixture prepareFixture() {
        Jpa.runInTransaction(em -> em.createQuery("delete from Customer").executeUpdate());
        Long id = Jpa.inTransaction(em -> {
            Customer c = new Customer("Olena", "Koval", "olena@example.com");
            em.persist(c);
            return c.getId();
        });
        Customer detached;
        try (EntityManager em = Jpa.emf().createEntityManager()) {
            detached = em.find(Customer.class, id);
        }
        return new Scenarios.Fixture(id, detached);
    }
}
