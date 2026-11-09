package kg.alatoo.payment.fixtures;

import kg.alatoo.payment.DuplicateIdempotencyKeyException;
import kg.alatoo.payment.PaymentRepositoryPort;
import kg.alatoo.payment.models.PageQuery;
import kg.alatoo.payment.models.PageResult;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.PaymentStatusChange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryPaymentRepository implements PaymentRepositoryPort {
    private final Map<UUID, Payment> payments = new ConcurrentHashMap<>();
    private final List<PaymentStatusChange> history = new ArrayList<>();
    private final AtomicLong historyIds = new AtomicLong();

    @Override
    public synchronized Payment saveWithHistory(Payment payment, PaymentStatusChange change) {
        boolean duplicate = payments.values().stream()
                .anyMatch(p -> !p.id().equals(payment.id())
                        && p.merchantId().equals(payment.merchantId())
                        && p.idempotencyKey().equals(payment.idempotencyKey()));
        if (duplicate) {
            throw new DuplicateIdempotencyKeyException("duplicate idempotency key", null);
        }
        payments.put(payment.id(), payment);
        history.add(new PaymentStatusChange(historyIds.incrementAndGet(), change.paymentId(), change.fromStatus(),
                change.toStatus(), change.changedAt(), change.note()));
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return Optional.ofNullable(payments.get(id));
    }

    @Override
    public Optional<Payment> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey) {
        return payments.values().stream()
                .filter(p -> p.merchantId().equals(merchantId) && p.idempotencyKey().equals(idempotencyKey))
                .findFirst();
    }

    @Override
    public PageResult<Payment> findByMerchant(UUID merchantId, PaymentStatus status, PageQuery pageQuery) {
        List<Payment> all = payments.values().stream()
                .filter(p -> p.merchantId().equals(merchantId))
                .filter(p -> status == null || p.status() == status)
                .sorted(Comparator.comparing(Payment::createdAt).reversed())
                .toList();
        List<Payment> page = all.stream()
                .skip((long) pageQuery.page() * pageQuery.size())
                .limit(pageQuery.size())
                .toList();
        return new PageResult<>(page, pageQuery.page(), pageQuery.size(), all.size());
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return payments.values().stream().filter(p -> p.status() == status).toList();
    }

    @Override
    public synchronized List<PaymentStatusChange> findHistory(UUID paymentId) {
        return history.stream().filter(h -> h.paymentId().equals(paymentId)).toList();
    }
}
