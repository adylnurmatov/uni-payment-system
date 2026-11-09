package kg.alatoo.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** balance == null — баланс сейчас не удалось получить из сети. */
public record MerchantSummary(UUID id, String name, String walletAddress, BigDecimal balance, Instant createdAt) {
}
