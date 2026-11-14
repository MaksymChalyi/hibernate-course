package ua.minibank.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletContext;
import ua.minibank.service.AccountService;
import ua.minibank.service.TransferService;

/**
 * Усі "біни" застосунку в одному місці. Створюються один раз при старті (AppContextListener)
 * і зберігаються в атрибуті ServletContext, звідки їх беруть servlet-и.
 * Це ручна версія того, що пізніше робитиме Spring IoC-контейнер.
 */
public record AppContext(EntityManagerFactory emf, ObjectMapper mapper,
                         AccountService accountService, TransferService transferService) {

    public static final String ATTRIBUTE = AppContext.class.getName();

    public static AppContext from(ServletContext servletContext) {
        AppContext context = (AppContext) servletContext.getAttribute(ATTRIBUTE);
        if (context == null) {
            throw new IllegalStateException("AppContext не ініціалізовано: перевір AppContextListener");
        }
        return context;
    }
}
