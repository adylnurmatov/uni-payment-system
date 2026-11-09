package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.models.PageQuery;
import kg.alatoo.payment.models.PageResult;
import kg.alatoo.payment.models.Payment;

@UseCase
public class ListPaymentsUseCase implements ListPaymentsPort {
    private final PaymentRepositoryPort paymentRepository;

    public ListPaymentsUseCase(PaymentRepositoryPort paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PageResult<Payment> execute(ListPaymentsQuery query) {
        return paymentRepository.findByMerchant(query.merchantId(), query.status(),
                new PageQuery(query.page(), query.size()));
    }
}
