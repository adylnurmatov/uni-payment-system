package kg.alatoo.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record GetClientBalanceResult(UUID clientId, String externalId, String address, BigDecimal balance) {
}
