package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.models.Wallet;

@UseCase
public class GetTreasuryUseCase implements GetTreasuryPort {
    private final WalletService walletService;

    public GetTreasuryUseCase(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public GetTreasuryResult execute() {
        Wallet treasury = walletService.getTreasury();
        return new GetTreasuryResult(treasury.id(), treasury.address(), walletService.getBalance(treasury));
    }
}
