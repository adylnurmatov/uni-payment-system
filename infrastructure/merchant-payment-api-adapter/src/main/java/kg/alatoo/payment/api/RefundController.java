package kg.alatoo.payment.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.alatoo.payment.BlockchainExplorer;
import kg.alatoo.payment.ListRefundsPort;
import kg.alatoo.payment.RefundPaymentCommand;
import kg.alatoo.payment.RefundPaymentPort;
import kg.alatoo.payment.auth.CurrentMerchant;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.rest.Amounts;
import kg.alatoo.payment.rest.Headers;
import kg.alatoo.payment.rest.OpenApiConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/{paymentId}/refunds")
@Tag(name = "Refunds", description = "Возвраты по подтверждённым платежам")
@SecurityRequirement(name = OpenApiConfiguration.MERCHANT_API_KEY)
public class RefundController {
    private final RefundPaymentPort refundPayment;
    private final ListRefundsPort listRefunds;
    private final BlockchainExplorer explorer;

    public RefundController(RefundPaymentPort refundPayment, ListRefundsPort listRefunds, BlockchainExplorer explorer) {
        this.refundPayment = refundPayment;
        this.listRefunds = listRefunds;
        this.explorer = explorer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Вернуть деньги клиенту",
            description = "Только для CONFIRMED / PARTIALLY_REFUNDED платежей. Без amount возвращается весь остаток. "
                    + "Отправляется новая транзакция с кошелька мерчанта на кошелёк клиента.")
    public RefundResponse create(@Parameter(hidden = true) @CurrentMerchant Merchant merchant,
                                 @PathVariable UUID paymentId,
                                 @RequestHeader(Headers.IDEMPOTENCY_KEY) String idempotencyKey,
                                 @RequestBody(required = false) RefundRequest request) {
        BigDecimal amount = request == null ? null : request.amount();
        Refund refund = refundPayment.execute(new RefundPaymentCommand(merchant, paymentId, amount, idempotencyKey));
        return RefundResponse.from(refund, explorer);
    }

    @GetMapping
    @Operation(summary = "Список возвратов по платежу")
    public List<RefundResponse> list(@Parameter(hidden = true) @CurrentMerchant Merchant merchant,
                                     @PathVariable UUID paymentId) {
        return listRefunds.execute(merchant.id(), paymentId).stream()
                .map(r -> RefundResponse.from(r, explorer))
                .toList();
    }

    public record RefundRequest(
            @Schema(description = "Сумма в ETH; если не указана — весь остаток", type = "string", example = "0.0005")
            BigDecimal amount) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RefundResponse(UUID refundId, UUID paymentId, String amount, String status, String txHash,
                                 String explorerUrl, String failureCode, String failureMessage,
                                 Instant createdAt, Instant updatedAt) {

        static RefundResponse from(Refund r, BlockchainExplorer explorer) {
            return new RefundResponse(r.id(), r.paymentId(), Amounts.format(r.amount()), r.status().name(),
                    r.txHash(), r.txHash() == null ? null : explorer.transactionUrl(r.txHash()),
                    r.failureCode() == null ? null : r.failureCode().name(), r.failureMessage(),
                    r.createdAt(), r.updatedAt());
        }
    }
}
