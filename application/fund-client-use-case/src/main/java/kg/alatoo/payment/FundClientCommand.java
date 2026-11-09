package kg.alatoo.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record FundClientCommand(UUID clientId, BigDecimal amount) {
}
