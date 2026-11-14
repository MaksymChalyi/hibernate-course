package ua.minibank;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Random;

/**
 * Формат виписки: account_number;booking_date;amount;currency;description
 * Перший рядок — заголовок.
 */
public final class Csv {

    public static final String HEADER = "account_number;booking_date;amount;currency;description";

    private static final String[] DESCRIPTIONS = {"Супермаркет", "Зарплата", "Оренда", "Кафе", "Таксі", "Комуналка"};

    private Csv() {
    }

    /** Генерує файл з rows рядків. Seed фіксований: при кожному запуску той самий файл. */
    public static void generate(Path file, int rows) {
        Random random = new Random(42);
        CurrencyCode[] currencies = CurrencyCode.values();
        LocalDate start = LocalDate.of(2026, 1, 1);
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write(HEADER);
            out.newLine();
            for (int i = 0; i < rows; i++) {
                String account = String.format("UA%06d", random.nextInt(1000));
                LocalDate date = start.plusDays(random.nextInt(365));
                BigDecimal amount = BigDecimal.valueOf(random.nextInt(2_000_000) - 1_000_000, 2)
                        .setScale(2, RoundingMode.UNNECESSARY);
                CurrencyCode currency = currencies[random.nextInt(currencies.length)];
                String description = DESCRIPTIONS[random.nextInt(DESCRIPTIONS.length)];
                out.write(account + ";" + date + ";" + amount.toPlainString() + ";" + currency + ";" + description);
                out.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static StatementLine parse(String line) {
        String[] p = line.split(";", -1);
        if (p.length != 5) {
            throw new IllegalArgumentException("Очікували 5 полів, а маємо " + p.length + ": " + line);
        }
        return new StatementLine(p[0], LocalDate.parse(p[1]), new BigDecimal(p[2]), CurrencyCode.valueOf(p[3]), p[4]);
    }
}
