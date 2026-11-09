package kg.alatoo.payment.models;

import lombok.With;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Раздача ETH из казны на кошелёк клиента или мерчанта.
 */
public record TreasuryTransfer(
        UUID id,
        UUID toWalletId,
        BigDecimal amount,
        String txHash,
        @With TreasuryTransferStatus status,
        Instant createdAt
) {
}
