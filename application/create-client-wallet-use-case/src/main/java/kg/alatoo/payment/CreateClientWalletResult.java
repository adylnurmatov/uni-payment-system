package kg.alatoo.payment;

import java.util.UUID;

public record CreateClientWalletResult(UUID clientId, UUID walletId, String walletAddress) {
}
