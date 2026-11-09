package kg.alatoo.payment;

import kg.alatoo.payment.entities.RefundEntity;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.models.RefundStatus;
import kg.alatoo.payment.repositories.RefundJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefundRepositoryAdapter implements RefundRepositoryPort {
    private final RefundJpaRepository repository;

    public RefundRepositoryAdapter(RefundJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Refund save(Refund refund) {
        try {
            return toModel(repository.saveAndFlush(toEntity(refund)));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdempotencyKeyException(
                    "Refund with idempotency key '" + refund.idempotencyKey() + "' already exists", e);
        }
    }

    @Override
    public Optional<Refund> findById(UUID id) {
        return repository.findById(id).map(RefundRepositoryAdapter::toModel);
    }

    @Override
    public Optional<Refund> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey) {
        return repository.findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey)
                .map(RefundRepositoryAdapter::toModel);
    }

    @Override
    public List<Refund> findByPaymentId(UUID paymentId) {
        return repository.findByPaymentIdOrderByCreatedAtAsc(paymentId).stream()
                .map(RefundRepositoryAdapter::toModel)
                .toList();
    }

    @Override
    public List<Refund> findByStatus(RefundStatus status) {
        return repository.findByStatus(status.name()).stream().map(RefundRepositoryAdapter::toModel).toList();
    }

    private static Refund toModel(RefundEntity e) {
        return new Refund(e.getId(), e.getPaymentId(), e.getAmount(), RefundStatus.valueOf(e.getStatus()),
                e.getTxHash(), e.getFailureCode() == null ? null : ErrorCode.valueOf(e.getFailureCode()),
                e.getFailureMessage(), e.getIdempotencyKey(), e.getRequestHash(), e.getSubmittedAt(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private static RefundEntity toEntity(Refund r) {
        return new RefundEntity(r.id(), r.paymentId(), r.amount(), r.status().name(), r.txHash(),
                r.failureCode() == null ? null : r.failureCode().name(), r.failureMessage(),
                r.idempotencyKey(), r.requestHash(), r.submittedAt(), r.createdAt(), r.updatedAt());
    }
}
