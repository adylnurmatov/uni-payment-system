package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.TreasuryTransfer;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.models.WalletType;
import kg.alatoo.payment.utils.Hashing;
import kg.alatoo.payment.utils.V7UUIDGenerator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Создаёт мерчанта: кошелёк + API-ключ (хранится хэш) + перевод ETH из казны на комиссии за возвраты.
 */
@Slf4j
@UseCase
public class CreateMerchantUseCase implements CreateMerchantPort {
    private final MerchantRepositoryPort merchantRepository;
    private final WalletService walletService;
    private final TreasuryFundingService treasuryFundingService;
    private final MerchantSettings settings;
    private final Clock clock;

    public CreateMerchantUseCase(MerchantRepositoryPort merchantRepository,
                                 WalletService walletService,
                                 TreasuryFundingService treasuryFundingService,
                                 MerchantSettings settings,
                                 Clock clock) {
        this.merchantRepository = merchantRepository;
        this.walletService = walletService;
        this.treasuryFundingService = treasuryFundingService;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public CreateMerchantResult execute(CreateMerchantCommand command) {
        String name = command.name() == null ? null : command.name().trim();
        if (name == null || name.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Merchant name is required");
        }
        BigDecimal funding = settings.initialFunding();
        boolean needsFunding = funding != null && funding.signum() > 0;

        // Проверяем казну ДО создания мерчанта, чтобы не плодить мерчантов без денег на комиссии.
        if (needsFunding) {
            treasuryFundingService.ensureCanFund(funding);
        }

        Wallet wallet = walletService.createWallet(WalletType.MERCHANT);
        String apiKey = ApiKeyGenerator.generate();
        Merchant merchant = merchantRepository.save(new Merchant(
                V7UUIDGenerator.generate(), name, Hashing.sha256Hex(apiKey), wallet.id(), clock.instant()));
        log.info("Merchant {} '{}' created with wallet {}", merchant.id(), name, wallet.address());

        String fundingTxHash = null;
        String fundingError = null;
        if (needsFunding) {
            // Мерчант уже создан, а API-ключ показывается только сейчас — поэтому ошибку перевода
            // не пробрасываем, а возвращаем в ответе (казну можно пополнить и повторить перевод позже).
            try {
                TreasuryTransfer transfer = treasuryFundingService.fund(wallet, funding);
                fundingTxHash = transfer.txHash();
            } catch (DomainException e) {
                log.warn("Merchant {} created, but initial funding failed: {}", merchant.id(), e.getMessage());
                fundingError = e.getCode() + ": " + e.getMessage();
            }
        }

        return new CreateMerchantResult(merchant.id(), merchant.name(), wallet.address(), apiKey,
                needsFunding ? funding : BigDecimal.ZERO, fundingTxHash, fundingError);
    }
}
