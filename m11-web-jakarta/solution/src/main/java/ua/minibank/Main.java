package ua.minibank;

/**
 * Запуск: Run Main, потім у терміналі:
 *   curl http://localhost:8080/accounts/1
 *   curl "http://localhost:8080/accounts/1/transactions?page=0&size=10"
 *   curl -X POST http://localhost:8080/transfers -H "Content-Type: application/json" \
 *        -d '{"fromAccountId":1,"toAccountId":3,"amount":250}'
 */
public class Main {

    public static void main(String[] args) {
        AppServer server = AppServer.start(8080);
        System.out.println("Mini Bank API: http://localhost:" + server.port() + "/accounts/1");
        server.await();
    }
}
