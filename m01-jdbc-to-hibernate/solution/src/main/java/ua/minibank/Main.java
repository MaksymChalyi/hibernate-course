package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Main {

    public static void main(String[] args) {
        // Спочатку Hibernate: hbm2ddl.auto=create перестворює таблицю customers.
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank")) {
            System.out.println("=== JDBC ===");
            JdbcCustomerDao jdbcDao = new JdbcCustomerDao();
            jdbcDao.createTableIfNotExists();
            Customer olena = jdbcDao.save(new Customer("Olena", "Koval", "olena@example.com"));
            System.out.println("Збережено через JDBC: " + olena);

            System.out.println("=== Hibernate ===");
            HibernateCustomerDao dao = new HibernateCustomerDao(emf);
            Customer ivan = dao.save(new Customer("Ivan", "Petrenko", "ivan@example.com"));
            System.out.println("Збережено через Hibernate: " + ivan);
            System.out.println("Знайдено: " + dao.findById(ivan.getId()));

            // Обидва DAO працюють з тією самою таблицею
            System.out.println("Усі клієнти (Hibernate): " + dao.findAll());
            System.out.println("Усі клієнти (JDBC):      " + jdbcDao.findAll());
        }
    }
}
