package kg.alatoo.payment;

import kg.alatoo.payment.models.Merchant;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param amount сумма возврата в ETH; null — вернуть весь остаток
 */
public record RefundPaymentCommand(
        Merchant merchant,
        UUID paymentId,
        BigDecimal amount,
        String idempotencyKey
) {
}
