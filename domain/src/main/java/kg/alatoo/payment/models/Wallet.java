package kg.alatoo.payment.models;

import kg.alatoo.payment.exceptions.DomainObjectInitializationException;

import java.time.Instant;
import java.util.UUID;

/**
 * Кошелёк, которым управляет сервис (кастодиальная модель).
 * Приватный ключ хранится только в зашифрованном виде.
 */
public record Wallet(
        UUID id,
        WalletType type,
        String address,
        String encryptedPrivateKey,
        Instant createdAt
) {

    public Wallet {
        if (id == null || type == null || createdAt == null) {
            throw new DomainObjectInitializationException("Wallet id, type and createdAt are required");
        }
        if (address == null || address.isBlank()) {
            throw new DomainObjectInitializationException("Wallet address is required");
        }
        if (encryptedPrivateKey == null || encryptedPrivateKey.isBlank()) {
            throw new DomainObjectInitializationException("Wallet encrypted private key is required");
        }
    }

    /** Никогда не печатаем ключ, даже зашифрованный. */
    @Override
    public String toString() {
        return "Wallet[id=" + id + ", type=" + type + ", address=" + address + "]";
    }
}
