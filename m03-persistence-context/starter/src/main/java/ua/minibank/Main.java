package ua.minibank;

import jakarta.persistence.EntityManager;

/**
 * Запусти і подивись у лог: скільки SQL виконалось і коли саме.
 */
public class Main {

    public static void main(String[] args) {
        Long id = Jpa.inTransaction(em -> {
            Customer c = new Customer("Olena", "Koval", "olena@example.com");
            em.persist(c);
            System.out.println("Після persist, до commit. id = " + c.getId() + ". INSERT у лозі ще немає");
            return c.getId();
        });
        System.out.println("Після commit: INSERT виконано");

        try (EntityManager em = Jpa.emf().createEntityManager()) {
            em.getTransaction().begin();
            Customer c = em.find(Customer.class, id);
            System.out.println("managed? " + em.contains(c));

            c.setLastName("Kovalenko");
            System.out.println("Змінили прізвище. UPDATE ще немає");

            em.flush();
            System.out.println("Після flush: UPDATE виконано, але транзакція ще не завершена");

            em.detach(c);
            System.out.println("managed після detach? " + em.contains(c));
            em.getTransaction().commit();
        }

        Jpa.emf().close();
    }
}
