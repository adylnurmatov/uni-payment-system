package kg.alatoo.payment.repositories;

import kg.alatoo.payment.entities.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletJpaRepository extends JpaRepository<WalletEntity, UUID> {

    Optional<WalletEntity> findFirstByType(String type);
}
