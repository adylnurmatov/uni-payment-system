package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.exceptions.InvalidStateException;
import kg.alatoo.payment.models.Client;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.models.WalletType;

import java.util.UUID;

@UseCase
public class CreateClientWalletUseCase implements CreateClientWalletPort {
    private final ClientRepositoryPort clientRepository;
    private final WalletService walletService;

    public CreateClientWalletUseCase(ClientRepositoryPort clientRepository, WalletService walletService) {
        this.clientRepository = clientRepository;
        this.walletService = walletService;
    }

    @Override
    public CreateClientWalletResult execute(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new DomainException(ErrorCode.CLIENT_NOT_FOUND, "Client " + clientId + " not found"));
        if (client.hasWallet()) {
            throw new InvalidStateException("Client " + clientId + " already has a wallet");
        }

        Wallet wallet = walletService.createWallet(WalletType.CLIENT);
        clientRepository.save(client.withWalletId(wallet.id()));

        return new CreateClientWalletResult(clientId, wallet.id(), wallet.address());
    }
}
