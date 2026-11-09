package kg.alatoo.payment;

import kg.alatoo.payment.models.TreasuryTransferStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record FundClientResult(
        UUID transferId,
        UUID clientId,
        String toAddress,
        BigDecimal amount,
        String txHash,
        TreasuryTransferStatus status
) {
}
