package ua.minibank;

import jakarta.persistence.AttributeConverter;

import java.util.Currency;

/**
 * Домашнє завдання 5. Власний конвертер: java.util.Currency ⇄ текст 'UAH' у базі.
 *
 * AttributeConverter<X, Y> — «як перетворити поле типу X на значення колонки типу Y і назад».
 * Hibernate викликає:
 *   convertToDatabaseColumn — перед INSERT/UPDATE (Java → база);
 *   convertToEntityAttribute — після SELECT (база → Java).
 *
 * Hibernate 6 вміє зберігати Currency і без конвертера, тож це вправа. Реальні випадки, коли конвертер
 * потрібен: "Y"/"N" замість boolean, сума в копійках (long) замість BigDecimal, номер телефону без пробілів.
 *
 * Кроки:
 *   5.1 Над класом — @Converter.
 *   5.2 Реалізуй обидва методи. Не забудь null: якщо прийшов null — повернути null.
 *       Currency → String:  currency.getCurrencyCode()
 *       String → Currency:  Currency.getInstance(code)
 *   5.3 Підключи до поля Account.currency: @Convert(converter = CurrencyConverter.class)
 *   5.4 Розкоментуй <class>ua.minibank.CurrencyConverter</class> у persistence.xml.
 * Перевірка: тест «5 · валюта зберігається кодом 'USD'» і зелені тести конвертера.
 */
// TODO 5.1 (ДЗ) Позначити клас @Converter
public class CurrencyConverter implements AttributeConverter<Currency, String> {

    @Override
    public String convertToDatabaseColumn(Currency currency) {
        // TODO 5.2 (ДЗ) Currency → "UAH"; null → null
        throw new UnsupportedOperationException("TODO 5.2: convertToDatabaseColumn");
    }

    @Override
    public Currency convertToEntityAttribute(String code) {
        // TODO 5.2 (ДЗ) "UAH" → Currency; null → null
        throw new UnsupportedOperationException("TODO 5.2: convertToEntityAttribute");
    }
}
