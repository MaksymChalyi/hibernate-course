package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

/**
 * Той самий DAO, що й JdbcCustomerDao, але на Hibernate.
 * Немає SQL, ResultSet і ручного mapping — Hibernate робить це за нас.
 */
public class HibernateCustomerDao {

    private final EntityManagerFactory emf;

    public HibernateCustomerDao(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Customer save(Customer customer) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                em.persist(customer);
                tx.commit();
                return customer;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    public Optional<Customer> findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return Optional.ofNullable(em.find(Customer.class, id));
        }
    }

    public List<Customer> findAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select c from Customer c order by c.id", Customer.class)
                    .getResultList();
        }
    }

    public void deleteById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Customer customer = em.find(Customer.class, id);
                if (customer != null) {
                    em.remove(customer);
                }
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }
}
