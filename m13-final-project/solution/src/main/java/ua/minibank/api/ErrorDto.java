package ua.minibank.api;

/** Тіло відповіді з помилкою: {"error": "INSUFFICIENT_FUNDS", "message": "..."} */
public record ErrorDto(String error, String message) {
}
