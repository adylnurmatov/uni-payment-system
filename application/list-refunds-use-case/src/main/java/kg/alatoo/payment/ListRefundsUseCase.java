package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Refund;

import java.util.List;
import java.util.UUID;

@UseCase
public class ListRefundsUseCase implements ListRefundsPort {
    private final PaymentRepositoryPort paymentRepository;
    private final RefundRepositoryPort refundRepository;

    public ListRefundsUseCase(PaymentRepositoryPort paymentRepository, RefundRepositoryPort refundRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    @Override
    public List<Refund> execute(UUID merchantId, UUID paymentId) {
        paymentRepository.findById(paymentId)
                .filter(p -> p.merchantId().equals(merchantId))
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND, "Payment " + paymentId + " not found"));
        return refundRepository.findByPaymentId(paymentId);
    }
}
