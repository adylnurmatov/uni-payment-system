package kg.alatoo.payment.services;

import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.models.RefundStatus;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Правила возвратов, не зависящие от БД и блокчейна:
 * <ul>
 *     <li>сумма всех не-FAILED возвратов не больше суммы платежа;</li>
 *     <li>статус платежа после возвратов считается по ЗАВЕРШЁННЫМ (COMPLETED) возвратам.</li>
 * </ul>
 */
@UtilityClass
public class RefundCalculator {

    /** Сколько ещё можно вернуть: сумма платежа минус все возвраты, кроме неудачных. */
    public static BigDecimal remaining(Payment payment, Collection<Refund> refunds) {
        BigDecimal reserved = refunds.stream()
                .filter(r -> r.status() != RefundStatus.FAILED)
                .map(Refund::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return payment.amount().subtract(reserved).max(BigDecimal.ZERO);
    }

    public static BigDecimal completedAmount(Collection<Refund> refunds) {
        return refunds.stream()
                .filter(r -> r.status() == RefundStatus.COMPLETED)
                .map(Refund::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Статус платежа после того, как очередной возврат подтвердился. */
    public static PaymentStatus statusAfterRefunds(Payment payment, Collection<Refund> refunds) {
        return completedAmount(refunds).compareTo(payment.amount()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public static boolean isRefundable(Payment payment) {
        return payment.status() == PaymentStatus.CONFIRMED || payment.status() == PaymentStatus.PARTIALLY_REFUNDED;
    }
}
