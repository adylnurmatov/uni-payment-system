package kg.alatoo.payment.fixtures;

import kg.alatoo.payment.ClientRepositoryPort;
import kg.alatoo.payment.models.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryClientRepository implements ClientRepositoryPort {
    private final Map<UUID, Client> clients = new ConcurrentHashMap<>();

    @Override
    public Client save(Client client) {
        clients.put(client.id(), client);
        return client;
    }

    @Override
    public Optional<Client> findById(UUID id) {
        return Optional.ofNullable(clients.get(id));
    }

    @Override
    public Optional<Client> findByExternalId(String externalId) {
        return clients.values().stream().filter(c -> c.externalId().equals(externalId)).findFirst();
    }

    @Override
    public boolean existsByExternalId(String externalId) {
        return findByExternalId(externalId).isPresent();
    }

    @Override
    public List<Client> findAll() {
        return new ArrayList<>(clients.values());
    }
}
