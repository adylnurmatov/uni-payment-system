package kg.alatoo.payment;

import kg.alatoo.payment.entities.ClientEntity;
import kg.alatoo.payment.models.Client;
import kg.alatoo.payment.repositories.ClientJpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ClientRepositoryAdapter implements ClientRepositoryPort {
    private final ClientJpaRepository repository;

    public ClientRepositoryAdapter(ClientJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Client save(Client client) {
        return toModel(repository.save(toEntity(client)));
    }

    @Override
    public Optional<Client> findById(UUID id) {
        return repository.findById(id).map(ClientRepositoryAdapter::toModel);
    }

    @Override
    public Optional<Client> findByExternalId(String externalId) {
        return repository.findByExternalId(externalId).map(ClientRepositoryAdapter::toModel);
    }

    @Override
    public boolean existsByExternalId(String externalId) {
        return repository.existsByExternalId(externalId);
    }

    @Override
    public List<Client> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(ClientRepositoryAdapter::toModel)
                .toList();
    }

    private static Client toModel(ClientEntity e) {
        return new Client(e.getId(), e.getExternalId(), e.getWalletId(), e.getCreatedAt());
    }

    private static ClientEntity toEntity(Client c) {
        return new ClientEntity(c.id(), c.externalId(), c.walletId(), c.createdAt());
    }
}
