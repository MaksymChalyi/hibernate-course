package ua.minibank.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletContext;
import ua.minibank.service.AccountService;
import ua.minibank.service.StatementImportService;
import ua.minibank.service.TransferService;

/**
 * Усі компоненти застосунку. Створюються в AppContextListener, живуть у ServletContext.
 */
public record AppContext(EntityManagerFactory emf, ObjectMapper mapper,
                         AccountService accountService, TransferService transferService,
                         StatementImportService statementImportService) {

    public static final String ATTRIBUTE = AppContext.class.getName();

    public static AppContext from(ServletContext servletContext) {
        AppContext context = (AppContext) servletContext.getAttribute(ATTRIBUTE);
        if (context == null) {
            throw new IllegalStateException("AppContext не ініціалізовано: перевір AppContextListener");
        }
        return context;
    }
}
