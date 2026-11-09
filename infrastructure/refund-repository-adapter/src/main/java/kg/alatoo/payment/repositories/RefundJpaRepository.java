package kg.alatoo.payment.repositories;

import kg.alatoo.payment.entities.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundJpaRepository extends JpaRepository<RefundEntity, UUID> {

    Optional<RefundEntity> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey);

    List<RefundEntity> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);

    List<RefundEntity> findByStatus(String status);
}
