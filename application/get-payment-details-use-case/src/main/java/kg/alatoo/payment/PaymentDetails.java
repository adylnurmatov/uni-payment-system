package kg.alatoo.payment;

import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatusChange;
import kg.alatoo.payment.models.Refund;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param refundable сколько ещё можно вернуть
 * @param canRefund  разрешён ли возврат сейчас (статус платежа и остаток)
 */
public record PaymentDetails(
        Payment payment,
        Merchant merchant,
        List<PaymentStatusChange> history,
        List<Refund> refunds,
        BigDecimal refundable,
        boolean canRefund
) {
}
