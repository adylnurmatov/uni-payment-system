package kg.alatoo.payment.fixtures;

import kg.alatoo.payment.DuplicateIdempotencyKeyException;
import kg.alatoo.payment.RefundRepositoryPort;
import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.models.RefundStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRefundRepository implements RefundRepositoryPort {
    private final Map<UUID, Refund> refunds = new ConcurrentHashMap<>();

    @Override
    public synchronized Refund save(Refund refund) {
        boolean duplicate = refunds.values().stream()
                .anyMatch(r -> !r.id().equals(refund.id())
                        && r.paymentId().equals(refund.paymentId())
                        && r.idempotencyKey().equals(refund.idempotencyKey()));
        if (duplicate) {
            throw new DuplicateIdempotencyKeyException("duplicate refund idempotency key", null);
        }
        refunds.put(refund.id(), refund);
        return refund;
    }

    @Override
    public Optional<Refund> findById(UUID id) {
        return Optional.ofNullable(refunds.get(id));
    }

    @Override
    public Optional<Refund> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey) {
        return refunds.values().stream()
                .filter(r -> r.paymentId().equals(paymentId) && r.idempotencyKey().equals(idempotencyKey))
                .findFirst();
    }

    @Override
    public List<Refund> findByPaymentId(UUID paymentId) {
        return refunds.values().stream()
                .filter(r -> r.paymentId().equals(paymentId))
                .sorted(Comparator.comparing(Refund::createdAt))
                .toList();
    }

    @Override
    public List<Refund> findByStatus(RefundStatus status) {
        return refunds.values().stream().filter(r -> r.status() == status).toList();
    }
}
