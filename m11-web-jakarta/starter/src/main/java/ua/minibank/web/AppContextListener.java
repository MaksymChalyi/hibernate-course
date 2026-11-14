package ua.minibank.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import ua.minibank.AppConfig;

/**
 * Tomcat викликає contextInitialized один раз при старті і contextDestroyed при зупинці.
 */
public class AppContextListener implements ServletContextListener {

    /**
     * TODO 1. Ініціалізація застосунку. Порядок важливий:
     *   1. AppConfig config = AppConfig.fromSystemProperties();
     *   2. Міграції Flyway:
     *        Flyway.configure().dataSource(url, user, password).schemas(schema).load().migrate();
     *   3. EntityManagerFactory: Persistence.createEntityManagerFactory("minibank", Map.of(...))
     *      з jakarta.persistence.jdbc.url/user/password і hibernate.default_schema з config.
     *      Питання: чому Flyway має виконатися ДО створення EntityManagerFactory?
     *   4. Сервіси AccountService і TransferService.
     *   5. new AppContext(...) → event.getServletContext().setAttribute(AppContext.ATTRIBUTE, context).
     * ObjectMapper уже налаштований нижче.
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        throw new UnsupportedOperationException("TODO: contextInitialized");
    }

    /**
     * TODO 2. Закрити EntityManagerFactory з AppContext. Що станеться з пулом з'єднань, якщо цього не зробити?
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
