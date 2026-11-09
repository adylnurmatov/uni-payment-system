package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Payment;

import java.util.UUID;

@UseCase
public class GetPaymentUseCase implements GetPaymentPort {
    private final PaymentRepositoryPort paymentRepository;

    public GetPaymentUseCase(PaymentRepositoryPort paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment execute(UUID merchantId, UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .filter(payment -> payment.merchantId().equals(merchantId))
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND, "Payment " + paymentId + " not found"));
    }
}
