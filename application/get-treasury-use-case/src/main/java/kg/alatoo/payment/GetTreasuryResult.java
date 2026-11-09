package kg.alatoo.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record GetTreasuryResult(UUID walletId, String address, BigDecimal balance) {
}
