package kg.alatoo.payment;

import kg.alatoo.payment.models.PageQuery;
import kg.alatoo.payment.models.PageResult;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.PaymentStatusChange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryPort {

    /**
     * Атомарно (в одной транзакции БД) сохраняет новое состояние платежа и запись истории статусов.
     *
     * @throws DuplicateIdempotencyKeyException если у мерчанта уже есть платёж с таким Idempotency-Key
     */
    Payment saveWithHistory(Payment payment, PaymentStatusChange change);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    /** Платежи мерчанта, новые сверху. status == null — без фильтра. */
    PageResult<Payment> findByMerchant(UUID merchantId, PaymentStatus status, PageQuery pageQuery);

    List<Payment> findByStatus(PaymentStatus status);

    List<PaymentStatusChange> findHistory(UUID paymentId);
}
