package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== JDBC ===");
        JdbcCustomerDao jdbcDao = new JdbcCustomerDao();
        jdbcDao.createTableIfNotExists();
        Customer olena = jdbcDao.save(new Customer("Olena", "Koval", "olena." + System.nanoTime() + "@example.com"));
        System.out.println("Збережено через JDBC: " + olena);
        System.out.println("Усі клієнти: " + jdbcDao.findAll());

        System.out.println("=== Hibernate ===");
        // TODO 2: заповни META-INF/persistence.xml, інакше тут буде помилка.
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank")) {
            HibernateCustomerDao dao = new HibernateCustomerDao(emf);
            Customer ivan = dao.save(new Customer("Ivan", "Petrenko", "ivan." + System.nanoTime() + "@example.com"));
            System.out.println("Збережено через Hibernate: " + ivan);
            System.out.println("Знайдено: " + dao.findById(ivan.getId()));
            System.out.println("Усі клієнти: " + dao.findAll());
        }
    }
}
