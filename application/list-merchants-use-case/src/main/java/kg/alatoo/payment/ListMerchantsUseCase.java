package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.Wallet;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@UseCase
public class ListMerchantsUseCase implements ListMerchantsPort {
    private final MerchantRepositoryPort merchantRepository;
    private final WalletService walletService;

    public ListMerchantsUseCase(MerchantRepositoryPort merchantRepository, WalletService walletService) {
        this.merchantRepository = merchantRepository;
        this.walletService = walletService;
    }

    @Override
    public List<MerchantSummary> execute() {
        return merchantRepository.findAll().stream().map(this::toSummary).toList();
    }

    private MerchantSummary toSummary(Merchant merchant) {
        Wallet wallet = walletService.getWallet(merchant.walletId());
        return new MerchantSummary(merchant.id(), merchant.name(), wallet.address(), balanceOrNull(wallet),
                merchant.createdAt());
    }

    private BigDecimal balanceOrNull(Wallet wallet) {
        try {
            return walletService.getBalance(wallet);
        } catch (DomainException e) {
            log.warn("Cannot get balance of {}: {}", wallet.address(), e.getMessage());
            return null;
        }
    }
}
