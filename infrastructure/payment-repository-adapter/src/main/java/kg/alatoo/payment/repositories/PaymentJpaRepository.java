package kg.alatoo.payment.repositories;

import kg.alatoo.payment.entities.PaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    Page<PaymentEntity> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<PaymentEntity> findByMerchantIdAndStatus(UUID merchantId, String status, Pageable pageable);

    List<PaymentEntity> findByStatus(String status);
}
