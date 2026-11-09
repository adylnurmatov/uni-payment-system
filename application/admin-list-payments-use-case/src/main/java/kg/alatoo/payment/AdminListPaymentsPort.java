package kg.alatoo.payment;

import kg.alatoo.payment.models.PageResult;
import kg.alatoo.payment.models.PaymentStatus;

public interface AdminListPaymentsPort {

    /** Платежи всех мерчантов, новые сверху. status == null — без фильтра. */
    PageResult<PaymentRow> execute(PaymentStatus status, int page, int size);
}
