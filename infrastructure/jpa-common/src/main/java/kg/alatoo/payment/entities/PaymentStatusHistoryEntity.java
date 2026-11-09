package kg.alatoo.payment.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_status_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    @Column(name = "from_status", length = 32)
    private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 32)
    private String toStatus;
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
    @Column(name = "note", columnDefinition = "text")
    private String note;
}
