package kg.alatoo.payment;

import kg.alatoo.payment.entities.PaymentEntity;
import kg.alatoo.payment.entities.PaymentStatusHistoryEntity;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.PageQuery;
import kg.alatoo.payment.models.PageResult;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.PaymentStatusChange;
import kg.alatoo.payment.repositories.PaymentJpaRepository;
import kg.alatoo.payment.repositories.PaymentStatusHistoryJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {
    private final PaymentJpaRepository paymentRepository;
    private final PaymentStatusHistoryJpaRepository historyRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository paymentRepository,
                                    PaymentStatusHistoryJpaRepository historyRepository) {
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
    }

    /** Платёж и запись истории сохраняются в одной транзакции БД: либо оба, либо ничего. */
    @Override
    @Transactional
    public Payment saveWithHistory(Payment payment, PaymentStatusChange change) {
        PaymentEntity saved;
        try {
            saved = paymentRepository.saveAndFlush(toEntity(payment));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdempotencyKeyException(
                    "Payment with idempotency key '" + payment.idempotencyKey() + "' already exists", e);
        }
        historyRepository.save(new PaymentStatusHistoryEntity(null, change.paymentId(),
                change.fromStatus() == null ? null : change.fromStatus().name(),
                change.toStatus().name(), change.changedAt(), change.note()));
        return toModel(saved);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return paymentRepository.findById(id).map(PaymentRepositoryAdapter::toModel);
    }

    @Override
    public Optional<Payment> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey) {
        return paymentRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey)
                .map(PaymentRepositoryAdapter::toModel);
    }

    @Override
    public PageResult<Payment> findByMerchant(UUID merchantId, PaymentStatus status, PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(pageQuery.page(), pageQuery.size(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentEntity> page = status == null
                ? paymentRepository.findByMerchantId(merchantId, pageable)
                : paymentRepository.findByMerchantIdAndStatus(merchantId, status.name(), pageable);
        return new PageResult<>(page.getContent().stream().map(PaymentRepositoryAdapter::toModel).toList(),
                pageQuery.page(), pageQuery.size(), page.getTotalElements());
    }

    @Override
    public PageResult<Payment> findAll(PaymentStatus status, PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(pageQuery.page(), pageQuery.size(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentEntity> page = status == null
                ? paymentRepository.findAll(pageable)
                : paymentRepository.findAllByStatus(status.name(), pageable);
        return new PageResult<>(page.getContent().stream().map(PaymentRepositoryAdapter::toModel).toList(),
                pageQuery.page(), pageQuery.size(), page.getTotalElements());
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status.name()).stream().map(PaymentRepositoryAdapter::toModel).toList();
    }

    @Override
    public List<PaymentStatusChange> findHistory(UUID paymentId) {
        return historyRepository.findByPaymentIdOrderByIdAsc(paymentId).stream()
                .map(h -> new PaymentStatusChange(h.getId(), h.getPaymentId(),
                        h.getFromStatus() == null ? null : PaymentStatus.valueOf(h.getFromStatus()),
                        PaymentStatus.valueOf(h.getToStatus()), h.getChangedAt(), h.getNote()))
                .toList();
    }

    private static Payment toModel(PaymentEntity e) {
        return new Payment(e.getId(), e.getMerchantId(), e.getClientId(), e.getClientExternalId(), e.getAmount(),
                e.getCurrency(), PaymentStatus.valueOf(e.getStatus()), e.getTxHash(),
                e.getFailureCode() == null ? null : ErrorCode.valueOf(e.getFailureCode()), e.getFailureMessage(),
                e.getIdempotencyKey(), e.getRequestHash(), e.getSubmittedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static PaymentEntity toEntity(Payment p) {
        return new PaymentEntity(p.id(), p.merchantId(), p.clientId(), p.clientExternalId(), p.amount(),
                p.currency(), p.status().name(), p.txHash(),
                p.failureCode() == null ? null : p.failureCode().name(), p.failureMessage(),
                p.idempotencyKey(), p.requestHash(), p.submittedAt(), p.createdAt(), p.updatedAt());
    }
}
