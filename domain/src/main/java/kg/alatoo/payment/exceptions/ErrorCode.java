package kg.alatoo.payment.exceptions;

/**
 * Коды ошибок, которые видит мерчант в поле "code" ответа.
 * HTTP-статус для каждого кода выбирается в GlobalExceptionHandler (boot-common),
 * домен про HTTP ничего не знает.
 */
public enum ErrorCode {
    UNAUTHORIZED,
    VALIDATION_ERROR,
    CLIENT_NOT_FOUND,
    CLIENT_ALREADY_EXISTS,
    WALLET_NOT_FOUND,
    PAYMENT_NOT_FOUND,
    CLIENT_BLOCKED,
    CLIENT_DATA_UNAVAILABLE,
    INSUFFICIENT_FUNDS,
    TREASURY_INSUFFICIENT_FUNDS,
    INVALID_AMOUNT,
    BLOCKCHAIN_ERROR,
    INVALID_STATE,
    REFUND_EXCEEDS_PAYMENT,
    IDEMPOTENCY_CONFLICT,
    CONFIRMATION_TIMEOUT,
    INTERNAL_ERROR
}
