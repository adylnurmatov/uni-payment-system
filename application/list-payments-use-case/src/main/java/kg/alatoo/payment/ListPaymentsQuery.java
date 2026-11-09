package kg.alatoo.payment;

import kg.alatoo.payment.models.PaymentStatus;

import java.util.UUID;

/** status == null — без фильтра. */
public record ListPaymentsQuery(UUID merchantId, PaymentStatus status, int page, int size) {
}
