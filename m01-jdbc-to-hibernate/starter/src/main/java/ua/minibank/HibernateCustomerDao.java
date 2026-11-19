package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

/**
 * Той самий DAO, що й JdbcCustomerDao, але на Hibernate. Порівнюй методи попарно.
 *
 * Два об'єкти, з якими працюємо:
 *
 *   EntityManagerFactory emf — «фабрика». Одна на весь застосунок, створюється в HibernateDemo.
 *                              Знає всі entity і вміє підключатися до бази. Аналог DataSource у JDBC.
 *
 *   EntityManager em         — «робоча сесія» з базою. Створюємо новий на кожну операцію:
 *                              emf.createEntityManager(). Обов'язково закриваємо (try-with-resources).
 *                              Аналог Connection у JDBC.
 *
 * Головні методи EntityManager:
 *   em.persist(obj)            — зберегти НОВИЙ об'єкт          (INSERT)
 *   em.find(Customer.class, id) — знайти за первинним ключем    (SELECT ... where id = ?)
 *   em.createQuery("JPQL", Customer.class).getResultList() — запит (SELECT)
 *   em.remove(obj)             — видалити                        (DELETE)
 *
 * Правило: усе, що ЗМІНЮЄ базу (persist, remove), — тільки всередині транзакції.
 * Читання (find, запити) можна без транзакції.
 */
public class HibernateCustomerDao {

    private final EntityManagerFactory emf;

    public HibernateCustomerDao(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /*
     * Завдання 3. Зберегти нового клієнта.
     *
     * JDBC (JdbcCustomerDao.save): Connection → PreparedStatement "insert into ..." → setString × 3 → id з результату.
     * Hibernate: один виклик em.persist(customer). INSERT напише Hibernate сам, id теж встановить сам.
     *
     * Шаблон методу. Перепиши його в метод (замість throw) і заповни ОДИН рядок у кроці 3:
     *
     *   try (EntityManager em = emf.createEntityManager()) {    // 1. відкрити сесію
     *       EntityTransaction tx = em.getTransaction();
     *       try {
     *           tx.begin();                                      // 2. почати транзакцію
     *           ...                                              // 3. зберегти customer — один рядок
     *           tx.commit();                                     // 4. зафіксувати — саме тут виконається INSERT
     *           return customer;                                 // 5. після commit у customer уже є id
     *       } catch (RuntimeException e) {
     *           if (tx.isActive()) {
     *               tx.rollback();                               // 6. помилка — скасувати все в транзакції
     *           }
     *           throw e;                                         // 7. і повідомити далі
     *       }
     *   }
     *
     * Перевірка: тести «3 · save ...» у HibernateCustomerDaoTest. У консолі має з'явитися "insert into m01.customers".
     */
    public Customer save(Customer customer) {
        // TODO 3 Зберегти клієнта через em.persist у транзакції (шаблон — у коментарі над методом)
        throw new UnsupportedOperationException("TODO 3: save() — див. коментар над методом");
    }

    /*
     * Завдання 4. Знайти клієнта за id.
     *
     * JDBC: "select ... where id = ?" + rs.next() + ручний map(rs).
     * Hibernate: em.find(Customer.class, id) — повертає готовий Customer або null, якщо такого id немає.
     *
     * Кроки:
     *   1. try (EntityManager em = emf.createEntityManager()) {
     *   2.     Customer customer = em.find(Customer.class, id);
     *   3.     return Optional.ofNullable(customer);   // null → Optional.empty(), інакше Optional.of(customer)
     *      }
     * Транзакція не потрібна: ми нічого не змінюємо.
     *
     * Перевірка: тести «4 · findById ...».
     */
    public Optional<Customer> findById(Long id) {
        // TODO 4 Знайти клієнта через em.find і загорнути результат в Optional
        throw new UnsupportedOperationException("TODO 4: findById() — див. коментар над методом");
    }

    /*
     * Завдання 5. Усі клієнти, відсортовані за id.
     *
     * Тут знадобиться JPQL — мова запитів Hibernate. Вона схожа на SQL, але пишемо імена
     * КЛАСІВ і ПОЛІВ, а не таблиць і колонок:
     *
     *   SQL:   select id, first_name, last_name, email from customers order by id
     *   JPQL:  select c from Customer c order by c.id
     *                    ^^^^^^^^ клас, а не таблиця customers
     *          "c" — псевдонім, як у SQL; "select c" означає «поверни цілі об'єкти Customer».
     *
     * Кроки:
     *   1. try (EntityManager em = emf.createEntityManager()) {
     *   2.     return em.createQuery("select c from Customer c order by c.id", Customer.class)
     *                   .getResultList();
     *      }
     * Другий аргумент Customer.class — тип результату, щоб отримати List<Customer>, а не List<Object>.
     *
     * Перевірка: тест «5 · findAll ...».
     */
    public List<Customer> findAll() {
        // TODO 5 Повернути всіх клієнтів JPQL-запитом select c from Customer c order by c.id
        throw new UnsupportedOperationException("TODO 5: findAll() — див. коментар над методом");
    }

    /*
     * Завдання 6. Видалити клієнта за id.
     *
     * em.remove(...) приймає ОБ'ЄКТ, а не id. Тому спочатку знаходимо клієнта, потім видаляємо.
     * Видалення змінює базу → потрібна транзакція (шаблон як у save).
     *
     * Кроки всередині транзакції (між tx.begin() і tx.commit()):
     *   1. Customer customer = em.find(Customer.class, id);
     *   2. if (customer != null) { em.remove(customer); }   // немає клієнта — нічого не робимо
     *
     * Перевірка: тест «6 · deleteById ...». У консолі: спочатку select, потім delete.
     */
    public void deleteById(Long id) {
        // TODO 6 Знайти клієнта і видалити через em.remove у транзакції
        throw new UnsupportedOperationException("TODO 6: deleteById() — див. коментар над методом");
    }
}
