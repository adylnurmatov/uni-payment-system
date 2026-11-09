package kg.alatoo.payment.models;

import kg.alatoo.payment.exceptions.DomainObjectInitializationException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.exceptions.InvalidStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Возврат по платежу. В блокчейне отмены нет, поэтому возврат — это новая транзакция
 * с кошелька мерчанта на кошелёк клиента (комиссию платит мерчант).
 * Переходы статусов проверяются прямо здесь: у возврата их всего четыре.
 */
public record Refund(
        UUID id,
        UUID paymentId,
        BigDecimal amount,
        RefundStatus status,
        String txHash,
        ErrorCode failureCode,
        String failureMessage,
        String idempotencyKey,
        String requestHash,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public Refund {
        if (id == null || paymentId == null || status == null || createdAt == null || updatedAt == null) {
            throw new DomainObjectInitializationException("Refund id, paymentId, status and timestamps are required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new DomainObjectInitializationException("Refund amount must be positive");
        }
        if (idempotencyKey == null || requestHash == null) {
            throw new DomainObjectInitializationException("Refund idempotency key and request hash are required");
        }
    }

    public static Refund create(UUID id, UUID paymentId, BigDecimal amount, String idempotencyKey,
                                String requestHash, Instant now) {
        return new Refund(id, paymentId, amount, RefundStatus.CREATED, null, null, null,
                idempotencyKey, requestHash, null, now, now);
    }

    /** CREATED → PENDING: транзакция отправлена. */
    public Refund markPending(String txHash, Instant now) {
        requireStatus(RefundStatus.CREATED, RefundStatus.PENDING);
        return new Refund(id, paymentId, amount, RefundStatus.PENDING, txHash, null, null,
                idempotencyKey, requestHash, now, createdAt, now);
    }

    /** PENDING → COMPLETED: транзакция подтверждена в блоке. */
    public Refund complete(Instant now) {
        requireStatus(RefundStatus.PENDING, RefundStatus.COMPLETED);
        return new Refund(id, paymentId, amount, RefundStatus.COMPLETED, txHash, null, null,
                idempotencyKey, requestHash, submittedAt, createdAt, now);
    }

    /** CREATED или PENDING → FAILED. */
    public Refund fail(ErrorCode code, String message, Instant now) {
        if (status != RefundStatus.CREATED && status != RefundStatus.PENDING) {
            throw new InvalidStateException("Refund " + id + " cannot move from " + status + " to FAILED");
        }
        return new Refund(id, paymentId, amount, RefundStatus.FAILED, txHash, code, message,
                idempotencyKey, requestHash, submittedAt, createdAt, now);
    }

    public boolean isFailed() {
        return status == RefundStatus.FAILED;
    }

    private void requireStatus(RefundStatus expected, RefundStatus target) {
        if (status != expected) {
            throw new InvalidStateException("Refund " + id + " cannot move from " + status + " to " + target);
        }
    }
}
