package ua.minibank;

/**
 * Заняття 1: згадуємо JDBC. Запусти (зелена стрілка біля main) і подивись, що виводиться.
 * Потім відкрий таблицю m01.customers у pgAdmin / DBeaver: рядки справді там.
 */
public class JdbcDemo {

    public static void main(String[] args) {
        JdbcCustomerDao dao = new JdbcCustomerDao();
        dao.createTableIfNotExists();

        // email має бути унікальним, тому додаємо до нього поточний час — щоб JdbcDemo можна було запускати багато разів
        Customer olena = dao.save(new Customer("Olena", "Koval", "olena." + System.nanoTime() + "@example.com"));
        System.out.println("Збережено: " + olena);

        System.out.println("Знайдено за id: " + dao.findById(olena.getId()));
        System.out.println("Усі клієнти: " + dao.findAll());
        System.out.println("Кількість клієнтів: " + dao.count());
    }
}
