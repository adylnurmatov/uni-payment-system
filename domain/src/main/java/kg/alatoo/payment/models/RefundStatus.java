package kg.alatoo.payment.models;

/**
 * Статусы возврата: CREATED → PENDING → COMPLETED | FAILED (FAILED возможен и из CREATED).
 */
public enum RefundStatus {
    CREATED,
    PENDING,
    COMPLETED,
    FAILED
}
