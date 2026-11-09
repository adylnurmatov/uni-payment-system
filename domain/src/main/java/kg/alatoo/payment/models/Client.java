package kg.alatoo.payment.models;

import kg.alatoo.payment.exceptions.DomainObjectInitializationException;
import lombok.With;

import java.time.Instant;
import java.util.UUID;

/**
 * Клиент мерчанта. Мерчанты обращаются к нему по externalId.
 * Кошелька может не быть (walletId == null).
 */
public record Client(
        UUID id,
        String externalId,
        @With UUID walletId,
        Instant createdAt
) {

    public Client {
        if (id == null || createdAt == null) {
            throw new DomainObjectInitializationException("Client id and createdAt are required");
        }
        if (externalId == null || externalId.isBlank()) {
            throw new DomainObjectInitializationException("Client externalId is required");
        }
    }

    public boolean hasWallet() {
        return walletId != null;
    }
}
