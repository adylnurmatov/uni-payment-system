package kg.alatoo.payment.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {
    @Id
    private UUID id;
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;
    @Column(name = "client_id")
    private UUID clientId;
    @Column(name = "client_external_id", nullable = false, length = 128)
    private String clientExternalId;
    @Column(name = "amount", nullable = false, precision = 36, scale = 18)
    private BigDecimal amount;
    @Column(name = "currency", nullable = false, length = 8)
    private String currency;
    @Column(name = "status", nullable = false, length = 32)
    private String status;
    @Column(name = "tx_hash", length = 66)
    private String txHash;
    @Column(name = "failure_code", length = 64)
    private String failureCode;
    @Column(name = "failure_message", columnDefinition = "text")
    private String failureMessage;
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
