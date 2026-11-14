package ua.minibank;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.time.YearMonth;

public class Main {

    public static void main(String[] args) {
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank")) {
            DemoData.seed(emf);
            ReportRepository reports = new ReportRepository(emf);

            System.out.println(reports.monthlyTurnover("UA01", YearMonth.of(2026, 1)));
            System.out.println(reports.monthlyTurnover("UA01", YearMonth.of(2026, 2)));
            reports.findTransactions("UA01", 0, 3).forEach(System.out::println);
        }
    }
}
