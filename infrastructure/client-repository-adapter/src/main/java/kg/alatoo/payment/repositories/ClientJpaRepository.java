package kg.alatoo.payment.repositories;

import kg.alatoo.payment.entities.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<ClientEntity, UUID> {

    Optional<ClientEntity> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}
