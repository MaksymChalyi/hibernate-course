package ua.minibank.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import ua.minibank.AppConfig;

/**
 * Tomcat викликає contextInitialized ОДИН раз при старті і contextDestroyed ОДИН раз при зупинці.
 * Тут створюється все «важке», що живе весь час роботи застосунку: міграції, EntityManagerFactory, сервіси.
 *
 * Аналогія з модулем 1: там фабрику створював main(), тут — Tomcat через цей listener.
 */
public class AppContextListener implements ServletContextListener {

    /*
     * Завдання 1. Зібрати застосунок при старті. Порядок кроків важливий.
     *
     * Імпорти: jakarta.persistence.EntityManagerFactory, jakarta.persistence.Persistence,
     *          org.flywaydb.core.Flyway, ua.minibank.service.AccountService, ua.minibank.service.TransferService,
     *          java.util.Map
     *
     * Крок 1. Налаштування підключення (за замовчуванням — локальний PostgreSQL, схема m11):
     *     AppConfig config = AppConfig.fromSystemProperties();
     *
     * Крок 2. Міграції Flyway — створюють таблиці з файлів src/main/resources/db/migration/V1__..., V2__...:
     *     Flyway.configure()
     *             .dataSource(config.dbUrl(), config.dbUser(), config.dbPassword())
     *             .schemas(config.dbSchema())
     *             .load()
     *             .migrate();
     *
     * Крок 3. EntityManagerFactory. Параметри з Map перекривають значення з persistence.xml:
     *     EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank", Map.of(
     *             "jakarta.persistence.jdbc.url", config.dbUrl(),
     *             "jakarta.persistence.jdbc.user", config.dbUser(),
     *             "jakarta.persistence.jdbc.password", config.dbPassword(),
     *             "hibernate.default_schema", config.dbSchema()));
     *
     *     Питання: чому крок 2 має бути ДО кроку 3? (Підказка: у persistence.xml hbm2ddl.auto = validate.)
     *
     * Крок 4. ObjectMapper уже створено нижче — не чіпай.
     *
     * Крок 5. Сервіси і контекст:
     *     AppContext context = new AppContext(emf, mapper, new AccountService(emf), new TransferService(emf));
     *     event.getServletContext().setAttribute(AppContext.ATTRIBUTE, context);
     *
     * Після цього servlet-и отримують сервіси через app() (див. JsonServlet).
     *
     * Перевірка: запусти Main. У консолі — «Mini Bank API: http://localhost:8080/accounts/1», без stack trace.
     * Якщо Tomcat пише «Застосунок не стартував» — шукай першу помилку вище в лозі.
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())                    // вміти серіалізувати Instant, LocalDate
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // дати як "2026-03-15T10:00:00Z", а не число

        // TODO 1 Flyway → EntityManagerFactory → сервіси → AppContext у ServletContext (кроки в коментарі вище)
        throw new UnsupportedOperationException("TODO 1: contextInitialized — див. коментар над методом");
    }

    /*
     * Завдання 2. Закрити EntityManagerFactory при зупинці.
     *
     *     Object context = event.getServletContext().getAttribute(AppContext.ATTRIBUTE);
     *     if (context instanceof AppContext app) {     // pattern matching: перевірка типу + змінна app
     *         app.emf().close();
     *     }
     *
     * Чому if: якщо старт впав (завдання 1 кинуло виняток), атрибута немає — і close() дав би NullPointerException.
     * Питання: що станеться з відкритими з'єднаннями до бази, якщо emf не закрити?
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // TODO 2 Закрити EntityManagerFactory (код у коментарі вище)
    }
}
