package kg.alatoo.payment.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import kg.alatoo.payment.BlockchainExplorer;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.rest.Amounts;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        UUID paymentId,
        String status,
        String clientId,
        String amount,
        String currency,
        String txHash,
        String explorerUrl,
        String failureCode,
        String failureMessage,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentResponse from(Payment payment, BlockchainExplorer explorer) {
        return new PaymentResponse(
                payment.id(),
                payment.status().name(),
                payment.clientExternalId(),
                Amounts.format(payment.amount()),
                payment.currency(),
                payment.txHash(),
                payment.txHash() == null ? null : explorer.transactionUrl(payment.txHash()),
                payment.failureCode() == null ? null : payment.failureCode().name(),
                payment.failureMessage(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }
}
