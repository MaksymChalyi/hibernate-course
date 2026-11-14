package ua.minibank;

import jakarta.persistence.EntityManager;

import static ua.minibank.Jpa.emf;
import static ua.minibank.Jpa.runInTransaction;

/**
 * Експерименти з Persistence Context. Перед кожним сценарієм у базі рівно один клієнт.
 * Завдання: для кожного сценарію спрогнозувати, скільки SQL-запитів виконає Hibernate,
 * і записати прогноз у Predictions. Тест ScenarioTest перевірить прогноз.
 *
 * Заняття: сценарії 1–5. Домашнє завдання: сценарії 6–10.
 */
public final class Scenarios {

    /** Те, що підготовлено перед сценарієм і не входить у підрахунок. */
    public record Fixture(Long customerId, Customer detachedCustomer) {
    }

    private Scenarios() {
    }

    /** 1. Двічі find того самого клієнта в одному EntityManager. */
    public static void s1(Fixture f) {
        try (EntityManager em = emf().createEntityManager()) {
            em.find(Customer.class, f.customerId());
            em.find(Customer.class, f.customerId());
        }
    }

    /** 2. find того самого клієнта у двох різних EntityManager. */
    public static void s2(Fixture f) {
        try (EntityManager em = emf().createEntityManager()) {
            em.find(Customer.class, f.customerId());
        }
        try (EntityManager em = emf().createEntityManager()) {
            em.find(Customer.class, f.customerId());
        }
    }

    /** 3. find, змінити прізвище, commit. Метод update/save не викликається. */
    public static void s3(Fixture f) {
        runInTransaction(em -> {
            Customer c = em.find(Customer.class, f.customerId());
            c.setLastName("Kovalenko");
        });
    }

    /** 4. find, записати в прізвище те саме значення, commit. */
    public static void s4(Fixture f) {
        runInTransaction(em -> {
            Customer c = em.find(Customer.class, f.customerId());
            c.setLastName(c.getLastName());
        });
    }

    /** 5. find, змінити прізвище, виконати JPQL-запит по клієнтах, rollback. */
    public static void s5(Fixture f) {
        try (EntityManager em = emf().createEntityManager()) {
            em.getTransaction().begin();
            Customer c = em.find(Customer.class, f.customerId());
            c.setLastName("Changed");
            em.createQuery("select c from Customer c where c.lastName = :name", Customer.class)
                    .setParameter("name", "Changed")
                    .getResultList();
            em.getTransaction().rollback();
        }
    }

    /** 6. find, em.clear(), знову find. */
    public static void s6(Fixture f) {
        try (EntityManager em = emf().createEntityManager()) {
            em.find(Customer.class, f.customerId());
            em.clear();
            em.find(Customer.class, f.customerId());
        }
    }

    /** 7. getReference і виклик лише getId(). */
    public static void s7(Fixture f) {
        try (EntityManager em = emf().createEntityManager()) {
            Customer reference = em.getReference(Customer.class, f.customerId());
            reference.getId();
        }
    }

    /** 8. find і remove у транзакції. */
    public static void s8(Fixture f) {
        runInTransaction(em -> em.remove(em.find(Customer.class, f.customerId())));
    }

    /** 9. JPQL-запит усіх клієнтів, потім find одного з них. */
    public static void s9(Fixture f) {
        try (EntityManager em = emf().createEntityManager()) {
            em.createQuery("select c from Customer c", Customer.class).getResultList();
            em.find(Customer.class, f.customerId());
        }
    }

    /** 10. merge detached-клієнта, у якого нічого не змінювали. */
    public static void s10(Fixture f) {
        runInTransaction(em -> em.merge(f.detachedCustomer()));
    }
}
