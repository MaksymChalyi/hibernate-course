package ua.minibank.api;

import ua.minibank.domain.Account;
import ua.minibank.domain.CurrencyCode;

import java.math.BigDecimal;

/**
 * Те, що бачить клієнт API. Entity назовні не віддаємо:
 *   - Jackson пройде по lazy-полях і отримає LazyInitializationException або N+1;
 *   - у JSON потраплять службові поля (version) і зайві дані;
 *   - будь-яке перейменування поля entity зламає контракт API.
 * Створюється всередині відкритого EntityManager, далі — звичайний незмінний об'єкт.
 */
public record AccountDto(Long id, String number, CurrencyCode currency, BigDecimal balance, String ownerName) {

    public static AccountDto from(Account account) {
        return new AccountDto(account.getId(), account.getNumber(), account.getCurrency(),
                account.getBalance(), account.getCustomer().getFullName());
    }
}
