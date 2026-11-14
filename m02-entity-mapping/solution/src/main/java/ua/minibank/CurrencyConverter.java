package ua.minibank;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Currency;

/**
 * Домашнє завдання: java.util.Currency ↔ "UAH".
 *
 * Hibernate 6 уміє мапити Currency і без конвертера. Власний конвертер потрібен,
 * коли в базі формат інший, ніж за замовчуванням: маскований номер телефону,
 * "Y"/"N" замість boolean, гроші в копійках тощо.
 */
@Converter
public class CurrencyConverter implements AttributeConverter<Currency, String> {

    @Override
    public String convertToDatabaseColumn(Currency currency) {
        return currency == null ? null : currency.getCurrencyCode();
    }

    @Override
    public Currency convertToEntityAttribute(String code) {
        return code == null ? null : Currency.getInstance(code);
    }
}
