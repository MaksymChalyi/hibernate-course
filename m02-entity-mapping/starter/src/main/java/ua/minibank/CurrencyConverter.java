package ua.minibank;

import jakarta.persistence.AttributeConverter;

import java.util.Currency;

/**
 * Домашнє завдання. TODO 5:
 *   - познач клас @Converter;
 *   - реалізуй обидва методи (Currency ↔ "UAH"), не забудь про null;
 *   - підключи конвертер до Account.currency через @Convert(converter = CurrencyConverter.class);
 *   - додай <class>ua.minibank.CurrencyConverter</class> у persistence.xml.
 */
public class CurrencyConverter implements AttributeConverter<Currency, String> {

    @Override
    public String convertToDatabaseColumn(Currency currency) {
        throw new UnsupportedOperationException("TODO: convertToDatabaseColumn");
    }

    @Override
    public Currency convertToEntityAttribute(String code) {
        throw new UnsupportedOperationException("TODO: convertToEntityAttribute");
    }
}
