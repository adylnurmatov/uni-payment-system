package kg.alatoo.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * @param walletAddress null — у клиента нет кошелька
 * @param balance       null — кошелька нет или баланс сейчас не удалось получить из сети
 */
public record ClientSummary(UUID id, String externalId, String walletAddress, BigDecimal balance, Instant createdAt) {
}
