package kg.alatoo.payment.exceptions;

import lombok.Getter;

import java.util.UUID;

/**
 * Возврат создан, но завершился ошибкой (status = FAILED).
 */
@Getter
public class RefundFailedException extends DomainException {
    private final UUID paymentId;
    private final UUID refundId;

    public RefundFailedException(ErrorCode code, String message, UUID paymentId, UUID refundId) {
        super(code, message);
        this.paymentId = paymentId;
        this.refundId = refundId;
    }
}
