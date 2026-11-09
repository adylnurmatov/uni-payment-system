package kg.alatoo.payment;

import java.util.UUID;

/** walletAddress == null, если кошелёк не создавался. */
public record CreateClientResult(UUID clientId, String externalId, String walletAddress) {
}
