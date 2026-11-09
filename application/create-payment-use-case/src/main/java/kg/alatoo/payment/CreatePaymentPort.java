package kg.alatoo.payment;

import kg.alatoo.payment.models.Payment;

public interface CreatePaymentPort {

    /**
     * Создаёт платёж и синхронно проходит шаги до отправки транзакции.
     *
     * @return платёж в статусе PENDING (с txHash)
     * @throws kg.alatoo.payment.exceptions.PaymentFailedException если платёж создан, но завершился ошибкой
     * @throws kg.alatoo.payment.exceptions.DomainException        INVALID_AMOUNT, VALIDATION_ERROR,
     *                                                             IDEMPOTENCY_CONFLICT (платёж не создаётся)
     */
    Payment execute(CreatePaymentCommand command);
}
