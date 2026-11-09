package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Client;
import kg.alatoo.payment.models.TreasuryTransfer;
import kg.alatoo.payment.models.Wallet;

/**
 * Пополнить кошелёк клиента из казны.
 */
@UseCase
public class FundClientUseCase implements FundClientPort {
    private final ClientRepositoryPort clientRepository;
    private final WalletService walletService;
    private final TreasuryFundingService treasuryFundingService;

    public FundClientUseCase(ClientRepositoryPort clientRepository,
                             WalletService walletService,
                             TreasuryFundingService treasuryFundingService) {
        this.clientRepository = clientRepository;
        this.walletService = walletService;
        this.treasuryFundingService = treasuryFundingService;
    }

    @Override
    public FundClientResult execute(FundClientCommand command) {
        Client client = clientRepository.findById(command.clientId())
                .orElseThrow(() -> new DomainException(ErrorCode.CLIENT_NOT_FOUND,
                        "Client " + command.clientId() + " not found"));
        if (!client.hasWallet()) {
            throw new DomainException(ErrorCode.WALLET_NOT_FOUND, "Client " + client.id() + " has no wallet");
        }
        Wallet wallet = walletService.getWallet(client.walletId());

        TreasuryTransfer transfer = treasuryFundingService.fund(wallet, command.amount());

        return new FundClientResult(transfer.id(), client.id(), wallet.address(), transfer.amount(),
                transfer.txHash(), transfer.status());
    }
}
