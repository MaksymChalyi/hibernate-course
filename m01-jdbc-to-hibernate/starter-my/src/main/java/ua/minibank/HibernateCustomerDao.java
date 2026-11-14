package ua.minibank;

import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Той самий DAO, що й JdbcCustomerDao, але на Hibernate.
 *
 * Правила:
 *   - один EntityManager на одну операцію, закривай його (EntityManager — AutoCloseable);
 *   - операції запису (save, deleteById) — в транзакції: begin → commit, при помилці rollback.
 */
public class HibernateCustomerDao {

    private final EntityManagerFactory emf;

    public HibernateCustomerDao(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Customer save(Customer customer) {
        // TODO 3: em.persist(customer) у транзакції. Після commit у customer з'явиться id.
        throw new UnsupportedOperationException("TODO: save");
    }

    public Optional<Customer> findById(Long id) {
        // TODO 4: em.find(Customer.class, id). Транзакція для читання не обов'язкова.
        throw new UnsupportedOperationException("TODO: findById");
    }

    public List<Customer> findAll() {
        // TODO 5: JPQL "select c from Customer c order by c.id".
        //  Зверни увагу: у запиті ім'я класу Customer, а не таблиці customers.
        throw new UnsupportedOperationException("TODO: findAll");
    }

    public void deleteById(Long id) {
        // TODO 6: знайти клієнта і викликати em.remove(...) у транзакції.
        //  Якщо клієнта немає — нічого не робити.
        throw new UnsupportedOperationException("TODO: deleteById");
    }
}
