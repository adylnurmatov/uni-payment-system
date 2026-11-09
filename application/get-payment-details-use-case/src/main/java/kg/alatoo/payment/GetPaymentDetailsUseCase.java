package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.services.RefundCalculator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@UseCase
public class GetPaymentDetailsUseCase implements GetPaymentDetailsPort {
    private final PaymentRepositoryPort paymentRepository;
    private final RefundRepositoryPort refundRepository;
    private final MerchantRepositoryPort merchantRepository;

    public GetPaymentDetailsUseCase(PaymentRepositoryPort paymentRepository,
                                    RefundRepositoryPort refundRepository,
                                    MerchantRepositoryPort merchantRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.merchantRepository = merchantRepository;
    }

    @Override
    public PaymentDetails execute(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND, "Payment " + paymentId + " not found"));
        Merchant merchant = merchantRepository.findById(payment.merchantId())
                .orElseThrow(() -> new DomainException(ErrorCode.MERCHANT_NOT_FOUND,
                        "Merchant " + payment.merchantId() + " not found"));
        List<Refund> refunds = refundRepository.findByPaymentId(paymentId);
        BigDecimal refundable = RefundCalculator.remaining(payment, refunds);
        boolean canRefund = RefundCalculator.isRefundable(payment) && refundable.signum() > 0;

        return new PaymentDetails(payment, merchant, paymentRepository.findHistory(paymentId), refunds,
                refundable, canRefund);
    }
}
