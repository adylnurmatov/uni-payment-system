package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Client;
import kg.alatoo.payment.models.Wallet;

import java.util.UUID;

@UseCase
public class GetClientBalanceUseCase implements GetClientBalancePort {
    private final ClientRepositoryPort clientRepository;
    private final WalletService walletService;

    public GetClientBalanceUseCase(ClientRepositoryPort clientRepository, WalletService walletService) {
        this.clientRepository = clientRepository;
        this.walletService = walletService;
    }

    @Override
    public GetClientBalanceResult execute(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new DomainException(ErrorCode.CLIENT_NOT_FOUND, "Client " + clientId + " not found"));
        if (!client.hasWallet()) {
            throw new DomainException(ErrorCode.WALLET_NOT_FOUND, "Client " + clientId + " has no wallet");
        }
        Wallet wallet = walletService.getWallet(client.walletId());
        return new GetClientBalanceResult(client.id(), client.externalId(), wallet.address(),
                walletService.getBalance(wallet));
    }
}
