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
@Table(name = "wallets")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WalletEntity {
    @Id
    private UUID id;
    @Column(name = "type", nullable = false, length = 16)
    private String type;
    @Column(name = "address", nullable = false, unique = true, length = 42)
    private String address;
    @Column(name = "encrypted_private_key", nullable = false, columnDefinition = "text")
    private String encryptedPrivateKey;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
