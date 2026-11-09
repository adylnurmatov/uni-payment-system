package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Client;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.models.WalletType;
import kg.alatoo.payment.utils.V7UUIDGenerator;

import java.time.Clock;

@UseCase
public class CreateClientUseCase implements CreateClientPort {
    private final ClientRepositoryPort clientRepository;
    private final WalletService walletService;
    private final Clock clock;

    public CreateClientUseCase(ClientRepositoryPort clientRepository, WalletService walletService, Clock clock) {
        this.clientRepository = clientRepository;
        this.walletService = walletService;
        this.clock = clock;
    }

    @Override
    public CreateClientResult execute(CreateClientCommand command) {
        String externalId = command.externalId() == null ? null : command.externalId().trim();
        if (externalId == null || externalId.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "externalId is required");
        }
        if (clientRepository.existsByExternalId(externalId)) {
            throw new DomainException(ErrorCode.CLIENT_ALREADY_EXISTS,
                    "Client with externalId '" + externalId + "' already exists");
        }

        Wallet wallet = command.createWallet() ? walletService.createWallet(WalletType.CLIENT) : null;
        Client client = new Client(V7UUIDGenerator.generate(), externalId,
                wallet == null ? null : wallet.id(), clock.instant());
        Client saved = clientRepository.save(client);

        return new CreateClientResult(saved.id(), saved.externalId(), wallet == null ? null : wallet.address());
    }
}
