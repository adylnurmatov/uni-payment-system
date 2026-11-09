package kg.alatoo.payment;

import kg.alatoo.payment.models.Refund;

public interface RefundPaymentPort {

    /**
     * @return возврат в статусе PENDING (транзакция мерчант → клиент отправлена)
     * @throws kg.alatoo.payment.exceptions.RefundFailedException возврат создан, но не удался (например, у мерчанта нет денег)
     * @throws kg.alatoo.payment.exceptions.DomainException       PAYMENT_NOT_FOUND, INVALID_STATE,
     *                                                            REFUND_EXCEEDS_PAYMENT, IDEMPOTENCY_CONFLICT
     */
    Refund execute(RefundPaymentCommand command);
}
