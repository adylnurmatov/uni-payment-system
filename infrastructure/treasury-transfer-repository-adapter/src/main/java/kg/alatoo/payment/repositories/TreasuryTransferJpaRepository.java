package kg.alatoo.payment.repositories;

import kg.alatoo.payment.entities.TreasuryTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TreasuryTransferJpaRepository extends JpaRepository<TreasuryTransferEntity, UUID> {

    List<TreasuryTransferEntity> findByStatus(String status);
}
