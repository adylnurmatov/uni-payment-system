package kg.alatoo.payment;

import kg.alatoo.payment.models.Payment;

import java.util.UUID;

public interface GetPaymentPort {

    /**
     * Платёж мерчанта. Чужой платёж выглядит так же, как несуществующий (PAYMENT_NOT_FOUND).
     */
    Payment execute(UUID merchantId, UUID paymentId);
}
