package kg.alatoo.payment.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Запись аудита: один переход статуса платежа (таблица payment_status_history).
 * fromStatus == null у самой первой записи (создание платежа).
 */
public record PaymentStatusChange(
        Long id,
        UUID paymentId,
        PaymentStatus fromStatus,
        PaymentStatus toStatus,
        Instant changedAt,
        String note
) {
}
