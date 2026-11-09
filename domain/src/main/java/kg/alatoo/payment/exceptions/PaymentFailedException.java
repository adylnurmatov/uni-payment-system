package kg.alatoo.payment.exceptions;

import lombok.Getter;

import java.util.UUID;

/**
 * Платёж создан, но завершился ошибкой (status = FAILED).
 * В ответе API кроме кода и сообщения отдаётся paymentId.
 */
@Getter
public class PaymentFailedException extends DomainException {
    private final UUID paymentId;

    public PaymentFailedException(ErrorCode code, String message, UUID paymentId) {
        super(code, message);
        this.paymentId = paymentId;
    }
}
