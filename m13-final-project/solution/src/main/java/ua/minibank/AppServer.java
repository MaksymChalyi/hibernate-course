package ua.minibank;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import ua.minibank.web.AccountServlet;
import ua.minibank.web.AppContextListener;
import ua.minibank.web.StatementServlet;
import ua.minibank.web.TransferServlet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Вбудований Tomcat: запускається з main(), без встановлення Tomcat і без WAR.
 * Реєстрація servlet-ів кодом замість web.xml або @WebServlet.
 */
public final class AppServer {

    private final Tomcat tomcat;

    private AppServer(Tomcat tomcat) {
        this.tomcat = tomcat;
    }

    /** port = 0 — випадковий вільний порт (зручно для тестів). */
    public static AppServer start(int port) {
        try {
            Path baseDir = Files.createTempDirectory("minibank-tomcat");
            Tomcat tomcat = new Tomcat();
            tomcat.setBaseDir(baseDir.toString());
            tomcat.setPort(port);
            tomcat.getConnector(); // створює HTTP-конектор

            Context context = tomcat.addContext("", baseDir.toAbsolutePath().toString());
            context.addApplicationListener(AppContextListener.class.getName());

            Tomcat.addServlet(context, "accounts", new AccountServlet());
            context.addServletMappingDecoded("/accounts/*", "accounts");
            Tomcat.addServlet(context, "transfers", new TransferServlet());
            context.addServletMappingDecoded("/transfers", "transfers");
            Tomcat.addServlet(context, "statements", new StatementServlet());
            context.addServletMappingDecoded("/statements", "statements");

            tomcat.start();
            if (!context.getState().isAvailable()) {
                tomcat.stop();
                throw new IllegalStateException("Застосунок не стартував — подивись помилку в лозі вище");
            }
            return new AppServer(tomcat);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (LifecycleException e) {
            throw new IllegalStateException("Не вдалося запустити Tomcat", e);
        }
    }

    public int port() {
        return tomcat.getConnector().getLocalPort();
    }

    public void await() {
        tomcat.getServer().await();
    }

    public void stop() {
        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (LifecycleException e) {
            throw new IllegalStateException(e);
        }
    }
}
