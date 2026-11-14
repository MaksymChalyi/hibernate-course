package ua.minibank.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.flywaydb.core.Flyway;
import ua.minibank.AppConfig;
import ua.minibank.service.AccountService;
import ua.minibank.service.TransferService;

import java.util.Map;

/**
 * Життєвий цикл застосунку:
 *   старт  → міграції Flyway → EntityManagerFactory (validate схеми) → сервіси → у ServletContext;
 *   зупинка → закрити EntityManagerFactory (і пул з'єднань разом з нею).
 *
 * Порядок важливий: Hibernate з hbm2ddl=validate впаде, якщо таблиць ще немає.
 */
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        AppConfig config = AppConfig.fromSystemProperties();

        Flyway.configure()
                .dataSource(config.dbUrl(), config.dbUser(), config.dbPassword())
                .schemas(config.dbSchema())
                .load()
                .migrate();

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank", Map.of(
                "jakarta.persistence.jdbc.url", config.dbUrl(),
                "jakarta.persistence.jdbc.user", config.dbUser(),
                "jakarta.persistence.jdbc.password", config.dbPassword(),
                "hibernate.default_schema", config.dbSchema()));

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        AppContext context = new AppContext(emf, mapper, new AccountService(emf), new TransferService(emf));
        event.getServletContext().setAttribute(AppContext.ATTRIBUTE, context);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object context = event.getServletContext().getAttribute(AppContext.ATTRIBUTE);
        if (context instanceof AppContext app) {
            app.emf().close();
        }
    }
}
