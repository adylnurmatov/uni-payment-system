package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.models.Client;
import kg.alatoo.payment.models.Wallet;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@UseCase
public class ListClientsUseCase implements ListClientsPort {
    private final ClientRepositoryPort clientRepository;
    private final WalletService walletService;

    public ListClientsUseCase(ClientRepositoryPort clientRepository, WalletService walletService) {
        this.clientRepository = clientRepository;
        this.walletService = walletService;
    }

    @Override
    public List<ClientSummary> execute() {
        return clientRepository.findAll().stream().map(this::toSummary).toList();
    }

    private ClientSummary toSummary(Client client) {
        if (!client.hasWallet()) {
            return new ClientSummary(client.id(), client.externalId(), null, null, client.createdAt());
        }
        Wallet wallet = walletService.getWallet(client.walletId());
        return new ClientSummary(client.id(), client.externalId(), wallet.address(), balanceOrNull(wallet),
                client.createdAt());
    }

    /** Одна недоступная RPC-нода не должна ронять всю страницу: показываем «—» вместо баланса. */
    private BigDecimal balanceOrNull(Wallet wallet) {
        try {
            return walletService.getBalance(wallet);
        } catch (DomainException e) {
            log.warn("Cannot get balance of {}: {}", wallet.address(), e.getMessage());
            return null;
        }
    }
}
