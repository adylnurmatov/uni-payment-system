package kg.alatoo.payment;

import kg.alatoo.payment.models.Client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepositoryPort {

    Client save(Client client);

    Optional<Client> findById(UUID id);

    Optional<Client> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Client> findAll();
}
