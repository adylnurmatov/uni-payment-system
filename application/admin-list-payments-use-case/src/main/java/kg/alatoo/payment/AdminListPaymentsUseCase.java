package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.PageQuery;
import kg.alatoo.payment.models.PageResult;
import kg.alatoo.payment.models.PaymentStatus;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@UseCase
public class AdminListPaymentsUseCase implements AdminListPaymentsPort {
    private final PaymentRepositoryPort paymentRepository;
    private final MerchantRepositoryPort merchantRepository;

    public AdminListPaymentsUseCase(PaymentRepositoryPort paymentRepository, MerchantRepositoryPort merchantRepository) {
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
    }

    @Override
    public PageResult<PaymentRow> execute(PaymentStatus status, int page, int size) {
        Map<UUID, String> merchantNames = merchantRepository.findAll().stream()
                .collect(Collectors.toMap(Merchant::id, Merchant::name, (a, b) -> a));
        return paymentRepository.findAll(status, new PageQuery(page, size))
                .map(p -> new PaymentRow(p, merchantNames.getOrDefault(p.merchantId(), "?")));
    }
}
