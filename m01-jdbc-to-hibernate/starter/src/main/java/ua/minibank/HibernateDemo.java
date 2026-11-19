package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Заняття 2: те саме, що JdbcDemo, але через Hibernate.
 *
 * Поки не зроблені завдання 1–6, запуск падає — це нормально. Читай ПЕРШИЙ рядок помилки:
 *   "UnsupportedOperationException: ..."          → метод ще не написаний, номер завдання є в тексті помилки;
 *   "Not an entity: class ua.minibank.Customer"  → не зроблено завдання 1 (@Entity) або 2.1 (<class>);
 *   "relation ... does not exist"                → не зроблено 2.2 (hbm2ddl.auto).
 * Виконав завдання → запустив → помилка «переїхала» далі. Коли все зроблено, падінь немає.
 *
 * Запусти і читай консоль: рядки, що починаються з "org.hibernate.SQL", — це SQL, який Hibernate
 * згенерував сам. Ми його не писали.
 */
public class HibernateDemo {

    public static void main(String[] args) {
        // Фабрика читає META-INF/persistence.xml (persistence unit "minibank"),
        // перевіряє анотації entity і підключається до бази.
        // hbm2ddl.auto=create → у цей момент у консолі з'являться "drop table" і "create table".
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank")) {
            HibernateCustomerDao dao = new HibernateCustomerDao(emf);

            Customer ivan = dao.save(new Customer("Ivan", "Petrenko", "ivan@example.com"));
            System.out.println("Збережено: " + ivan);

            System.out.println("Знайдено за id: " + dao.findById(ivan.getId()));

            dao.save(new Customer("Maria", "Shevchenko", "maria@example.com"));
            System.out.println("Усі клієнти: " + dao.findAll());

            dao.deleteById(ivan.getId());
            System.out.println("Після видалення: " + dao.findAll());
        }
    }
}
