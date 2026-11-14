package ua.minibank;

public class CustomerService {

    /**
     * TODO 2. Змінити прізвище клієнта.
     * Умова: без em.merge() і без будь-якого "update". Тест перевіряє, що виконано рівно SELECT + UPDATE.
     * Підказка: Jpa.runInTransaction(em -> ...).
     */
    public void rename(Long customerId, String newLastName) {
        throw new UnsupportedOperationException("TODO: rename");
    }

    /**
     * TODO 3 (резерв). detached — об'єкт, завантажений в іншому, вже закритому EntityManager.
     * Встанови йому новий email і збережи через em.merge(). Поверни результат merge.
     * Питання: це той самий об'єкт, що й detached?
     */
    public Customer changeEmail(Customer detached, String newEmail) {
        throw new UnsupportedOperationException("TODO: changeEmail");
    }
}
