package kg.alatoo.payment.repositories;

import kg.alatoo.payment.entities.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, UUID> {

    Optional<MerchantEntity> findByApiKeyHash(String apiKeyHash);
}
