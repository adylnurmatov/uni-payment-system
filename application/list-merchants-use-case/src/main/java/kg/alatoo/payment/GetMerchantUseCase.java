package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Merchant;

import java.util.UUID;

/**
 * Мерчант по id — нужен админ-панели, чтобы от имени мерчанта провести тестовую оплату
 * или возврат через те же use case, что и REST API.
 */
@UseCase
public class GetMerchantUseCase implements GetMerchantPort {
    private final MerchantRepositoryPort merchantRepository;

    public GetMerchantUseCase(MerchantRepositoryPort merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    public Merchant execute(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new DomainException(ErrorCode.MERCHANT_NOT_FOUND, "Merchant " + merchantId + " not found"));
    }
}
