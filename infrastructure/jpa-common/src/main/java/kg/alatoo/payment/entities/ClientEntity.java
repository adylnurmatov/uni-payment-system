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
@Table(name = "clients")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClientEntity {
    @Id
    private UUID id;
    @Column(name = "external_id", nullable = false, unique = true, length = 128)
    private String externalId;
    @Column(name = "wallet_id")
    private UUID walletId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
