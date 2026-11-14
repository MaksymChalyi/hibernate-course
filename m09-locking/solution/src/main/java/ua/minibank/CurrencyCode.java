package ua.minibank;

/**
 * Валюти, з якими працює банк. Enum замість java.util.Currency:
 * набір валют скінченний і відомий, а помилка в коді валюти ловиться компілятором.
 */
public enum CurrencyCode {
    UAH,
    USD,
    EUR
}
