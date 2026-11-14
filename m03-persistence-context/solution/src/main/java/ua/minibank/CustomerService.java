package ua.minibank;

/**
 * Операції з клієнтом, які показують dirty checking і merge.
 */
public class CustomerService {

    /**
     * Змінити прізвище. Ні update(), ні merge() не потрібні:
     * об'єкт із find — managed, Hibernate сам побачить зміну при commit.
     */
    public void rename(Long customerId, String newLastName) {
        Jpa.runInTransaction(em -> {
            Customer customer = em.find(Customer.class, customerId);
            if (customer == null) {
                throw new IllegalArgumentException("Клієнта " + customerId + " не знайдено");
            }
            customer.setLastName(newLastName);
        });
    }

    /**
     * Змінити email у detached-об'єкті (наприклад, прийшов із форми після закриття EntityManager).
     * merge повертає ІНШИЙ об'єкт — managed-копію. Переданий detached так і лишається detached:
     * подальші зміни в ньому Hibernate не відстежує.
     */
    public Customer changeEmail(Customer detached, String newEmail) {
        detached.setEmail(newEmail);
        return Jpa.inTransaction(em -> em.merge(detached));
    }
}
