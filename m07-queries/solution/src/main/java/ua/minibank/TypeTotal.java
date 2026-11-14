package ua.minibank;

import java.math.BigDecimal;

/** Сума і кількість операцій одного типу. Типи компонентів збігаються з результатом sum() і count(). */
public record TypeTotal(TransactionType type, BigDecimal total, Long count) {
}
