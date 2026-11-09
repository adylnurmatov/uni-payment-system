package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.utils.Hashing;

/**
 * Ищет мерчанта по хэшу API-ключа: сам ключ в БД не хранится.
 */
@UseCase
public class AuthenticateMerchantUseCase implements AuthenticateMerchantPort {
    private final MerchantRepositoryPort merchantRepository;

    public AuthenticateMerchantUseCase(MerchantRepositoryPort merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    public Merchant execute(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "X-Api-Key header is required");
        }
        return merchantRepository.findByApiKeyHash(Hashing.sha256Hex(apiKey.trim()))
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHORIZED, "Invalid API key"));
    }
}
