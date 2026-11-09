package kg.alatoo.payment;

import kg.alatoo.payment.models.Merchant;

import java.math.BigDecimal;

/**
 * @param merchant         аутентифицированный мерчант (по X-Api-Key)
 * @param clientExternalId clientId из запроса — внешний id клиента
 * @param amount           сумма в ETH
 * @param idempotencyKey   заголовок Idempotency-Key
 */
public record CreatePaymentCommand(
        Merchant merchant,
        String clientExternalId,
        BigDecimal amount,
        String idempotencyKey
) {
}
