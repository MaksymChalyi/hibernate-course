package ua.minibank;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Оборот по рахунку за місяць. */
public record MonthlyTurnover(String accountNumber, YearMonth month, BigDecimal totalCredit,
                              BigDecimal totalDebit, long transactionCount) {
}
