package kg.alatoo.payment.repositories;

import kg.alatoo.payment.entities.PaymentStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentStatusHistoryJpaRepository extends JpaRepository<PaymentStatusHistoryEntity, Long> {

    List<PaymentStatusHistoryEntity> findByPaymentIdOrderByIdAsc(UUID paymentId);
}
