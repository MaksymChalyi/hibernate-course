package ua.minibank.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.minibank.api.ErrorDto;
import ua.minibank.api.TransactionFilter;
import ua.minibank.domain.TransactionType;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * GET /accounts/{id}
 * GET /accounts/{id}/transactions?page=0&size=20&type=DEBIT&from=2026-01-01&to=2026-01-31&minAmount=100&maxAmount=5000
 *
 * from і to — календарні дати за Києвом, обидві включно: to=2026-01-31 охоплює весь день 31 січня.
 */
public class AccountServlet extends JsonServlet {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        String[] parts = path == null ? new String[0] : path.substring(1).split("/");

        if (parts.length == 1 && !parts[0].isEmpty()) {
            handle(response, 200, () -> app().accountService().getAccount(parseId(parts[0])));
        } else if (parts.length == 2 && parts[1].equals("transactions")) {
            handle(response, 200, () -> app().accountService().history(
                    parseId(parts[0]),
                    filter(request),
                    intParam(request, "page", 0),
                    intParam(request, "size", 20)));
        } else {
            writeJson(response, 404, new ErrorDto("NOT_FOUND", "Невідомий шлях"));
        }
    }

    private static TransactionFilter filter(HttpServletRequest request) {
        String type = request.getParameter("type");
        String from = request.getParameter("from");
        String to = request.getParameter("to");
        String min = request.getParameter("minAmount");
        String max = request.getParameter("maxAmount");
        return new TransactionFilter(
                type == null ? null : parseType(type),
                from == null ? null : startOfDay(parseDate(from)),
                to == null ? null : startOfDay(parseDate(to).plusDays(1)),
                min == null ? null : new BigDecimal(min),
                max == null ? null : new BigDecimal(max));
    }

    private static TransactionType parseType(String value) {
        try {
            return TransactionType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("type має бути DEBIT або CREDIT");
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Дата має бути у форматі YYYY-MM-DD: " + value);
        }
    }

    private static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(KYIV).toInstant();
    }

    private static Long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некоректний id: " + value);
        }
    }

    private static int intParam(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Параметр " + name + " має бути числом");
        }
    }
}
