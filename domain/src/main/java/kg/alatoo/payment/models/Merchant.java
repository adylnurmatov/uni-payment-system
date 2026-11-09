package kg.alatoo.payment.models;

import kg.alatoo.payment.exceptions.DomainObjectInitializationException;

import java.time.Instant;
import java.util.UUID;

/**
 * Мерчант — внешний сервис, который списывает деньги со своих клиентов.
 * Храним только хэш API-ключа, сам ключ показывается один раз при создании.
 */
public record Merchant(
        UUID id,
        String name,
        String apiKeyHash,
        UUID walletId,
        Instant createdAt
) {

    public Merchant {
        if (id == null || walletId == null || createdAt == null) {
            throw new DomainObjectInitializationException("Merchant id, walletId and createdAt are required");
        }
        if (name == null || name.isBlank()) {
            throw new DomainObjectInitializationException("Merchant name is required");
        }
        if (apiKeyHash == null || apiKeyHash.isBlank()) {
            throw new DomainObjectInitializationException("Merchant api key hash is required");
        }
    }

    @Override
    public String toString() {
        return "Merchant[id=" + id + ", name=" + name + ", walletId=" + walletId + "]";
    }
}
