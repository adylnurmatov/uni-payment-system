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
@Table(name = "treasury_transfers")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TreasuryTransferEntity {
    @Id
    private UUID id;
    @Column(name = "to_wallet_id", nullable = false)
    private UUID toWalletId;
    @Column(name = "amount", nullable = false, precision = 36, scale = 18)
    private BigDecimal amount;
    @Column(name = "tx_hash", length = 66)
    private String txHash;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
