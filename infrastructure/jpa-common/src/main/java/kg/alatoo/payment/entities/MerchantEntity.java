package kg.alatoo.payment.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantEntity {
    @Id
    private UUID id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "api_key_hash", nullable = false, unique = true, length = 64)
    private String apiKeyHash;
    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
