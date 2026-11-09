package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.models.WalletType;
import lombok.extern.slf4j.Slf4j;

/**
 * Создаёт казну при первом старте приложения. Если казна уже есть — ничего не делает.
 */
@Slf4j
@UseCase
public class InitTreasuryUseCase implements InitTreasuryPort {
    private final WalletService walletService;

    public InitTreasuryUseCase(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Wallet execute() {
        return walletService.findTreasury()
                .map(existing -> {
                    log.info("Treasury wallet: {}", existing.address());
                    return existing;
                })
                .orElseGet(() -> {
                    Wallet created = walletService.createWallet(WalletType.TREASURY);
                    log.info("Treasury wallet created: {}. Top it up with test ETH from the faucet.", created.address());
                    return created;
                });
    }
}
