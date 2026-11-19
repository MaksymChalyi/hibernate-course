package ua.minibank;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Розминка JDBC (завдання 0). Працює без Hibernate: можна запускати з першої хвилини заняття.
 */
@DisplayName("Модуль 1 · JDBC")
class JdbcCustomerDaoTest {

    private final JdbcCustomerDao dao = new JdbcCustomerDao();

    @BeforeAll
    static void createTable() {
        new JdbcCustomerDao().createTableIfNotExists();
    }

    @Test
    @DisplayName("0 · JDBC: count() рахує клієнтів у таблиці")
    void countReturnsNumberOfRows() throws Exception {
        try (Connection c = DriverManager.getConnection(Db.URL, Db.USER, Db.PASSWORD);
             Statement st = c.createStatement()) {
            st.execute("truncate table m01.customers restart identity");
        }
        assertEquals(0, dao.count(), "Порожня таблиця — має бути 0");

        dao.save(new Customer("Olena", "Koval", "olena@example.com"));
        dao.save(new Customer("Ivan", "Petrenko", "ivan@example.com"));

        assertEquals(2, dao.count(), "Після двох save має бути 2");
    }
}
