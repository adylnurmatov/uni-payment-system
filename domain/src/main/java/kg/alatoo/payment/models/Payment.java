package kg.alatoo.payment.models;

import kg.alatoo.payment.exceptions.DomainObjectInitializationException;
import kg.alatoo.payment.exceptions.ErrorCode;
import lombok.With;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Платёж: перевод с кошелька клиента на кошелёк мерчанта.
 * Статус меняется только через {@link kg.alatoo.payment.services.PaymentStateMachine}.
 */
public record Payment(
        UUID id,
        UUID merchantId,
        @With UUID clientId,
        String clientExternalId,
        BigDecimal amount,
        String currency,
        @With PaymentStatus status,
        @With String txHash,
        @With ErrorCode failureCode,
        @With String failureMessage,
        String idempotencyKey,
        String requestHash,
        @With Instant submittedAt,
        Instant createdAt,
        @With Instant updatedAt
) {
    public static final String CURRENCY_ETH = "ETH";

    public Payment {
        if (id == null || merchantId == null || status == null || createdAt == null || updatedAt == null) {
            throw new DomainObjectInitializationException("Payment id, merchantId, status and timestamps are required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new DomainObjectInitializationException("Payment amount must be positive");
        }
        if (idempotencyKey == null || requestHash == null) {
            throw new DomainObjectInitializationException("Payment idempotency key and request hash are required");
        }
    }

    /** Новый платёж в статусе CREATED. */
    public static Payment create(UUID id, UUID merchantId, String clientExternalId, BigDecimal amount,
                                 String idempotencyKey, String requestHash, Instant now) {
        return new Payment(id, merchantId, null, clientExternalId, amount, CURRENCY_ETH,
                PaymentStatus.CREATED, null, null, null, idempotencyKey, requestHash, null, now, now);
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }
}
