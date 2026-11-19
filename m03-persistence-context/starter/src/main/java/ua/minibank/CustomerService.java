package ua.minibank;

/**
 * Сервіс для двох операцій, на яких видно життєвий цикл entity:
 * зміна managed-об'єкта (dirty checking) і збереження detached-об'єкта (merge).
 */
public class CustomerService {

    /*
     * Завдання 2. Змінити прізвище клієнта.
     *
     * Головна умова: БЕЗ em.merge() і без будь-якого «save/update».
     * Тест рахує SQL і очікує рівно 2 запити: SELECT + UPDATE.
     *
     * Чому це працює: об'єкт, отриманий через em.find, — managed («під наглядом» EntityManager).
     * Hibernate запам'ятав, яким він був. При commit порівняє поточні значення зі знімком,
     * побачить зміну прізвища і сам виконає UPDATE. Це називається dirty checking.
     *
     * Кроки:
     *   1. Jpa.runInTransaction(em -> {          — готовий helper: відкриває EntityManager, begin, commit
     *   2.     Customer customer = em.find(Customer.class, customerId);
     *   3.     if (customer == null) {
     *              throw new IllegalArgumentException("Клієнта " + customerId + " не знайдено");
     *          }
     *   4.     customer.setLastName(newLastName);   — і все. Жодного «збережи»
     *      });
     *
     * Перевірка: тест «2 · rename(): лише SELECT + UPDATE».
     */
    public void rename(Long customerId, String newLastName) {
        // TODO 2 Змінити прізвище лише через setLastName на managed-об'єкті (кроки — над методом)
        throw new UnsupportedOperationException("TODO 2: rename() — див. коментар над методом");
    }

    /*
     * Завдання 3 (резерв). Змінити email у detached-об'єкті і зберегти його через merge.
     *
     * detached — об'єкт, який колись завантажили, але його EntityManager уже закритий.
     * Типова ситуація: дані прийшли з форми або з іншого запиту. Hibernate за ним не стежить.
     *
     * merge(detached) робить так:
     *   - завантажує з бази managed-копію з тим самим id (SELECT);
     *   - копіює в неї всі поля з detached;
     *   - ПОВЕРТАЄ цю managed-копію. Переданий об'єкт так і лишається detached.
     *
     * Кроки:
     *   1. detached.setEmail(newEmail);                          — змінюємо поза EntityManager
     *   2. return Jpa.inTransaction(em -> em.merge(detached));   — merge + commit, повертаємо результат merge
     *
     * Питання після тесту: merged == detached? Чому?
     * Перевірка: тест «3 (резерв) · merge повертає іншу, managed-копію».
     */
    public Customer changeEmail(Customer detached, String newEmail) {
        // TODO 3 (резерв) Змінити email у detached-об'єкті і зберегти через em.merge, повернути результат merge
        throw new UnsupportedOperationException("TODO 3: changeEmail() — див. коментар над методом");
    }
}
