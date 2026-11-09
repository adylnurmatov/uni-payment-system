package kg.alatoo.payment;

import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.models.RefundStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepositoryPort {

    /**
     * @throws DuplicateIdempotencyKeyException если по платежу уже есть возврат с таким Idempotency-Key
     */
    Refund save(Refund refund);

    Optional<Refund> findById(UUID id);

    Optional<Refund> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey);

    /** Возвраты платежа, старые сверху. */
    List<Refund> findByPaymentId(UUID paymentId);

    List<Refund> findByStatus(RefundStatus status);
}
