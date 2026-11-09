package kg.alatoo.payment;

import java.util.UUID;

public interface GetPaymentDetailsPort {

    /** Платёж с историей статусов и возвратами (для админ-панели). */
    PaymentDetails execute(UUID paymentId);
}
