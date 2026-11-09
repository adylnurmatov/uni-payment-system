package kg.alatoo.payment.models;

/**
 * Статусы платежа. Допустимые переходы описаны в {@link kg.alatoo.payment.services.PaymentStateMachine}.
 */
public enum PaymentStatus {
    CREATED,
    CLIENT_VERIFIED,
    CLIENT_DATA_RECEIVED,
    BALANCE_CHECKED,
    PENDING,
    CONFIRMED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    FAILED
}
