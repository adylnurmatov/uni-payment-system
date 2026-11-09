package kg.alatoo.payment;

import kg.alatoo.payment.models.Refund;

import java.util.List;
import java.util.UUID;

public interface ListRefundsPort {

    /** Возвраты по платежу мерчанта. Чужой платёж → PAYMENT_NOT_FOUND. */
    List<Refund> execute(UUID merchantId, UUID paymentId);
}
