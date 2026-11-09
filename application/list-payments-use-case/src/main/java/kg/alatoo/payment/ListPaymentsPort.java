package kg.alatoo.payment;

import kg.alatoo.payment.models.PageResult;
import kg.alatoo.payment.models.Payment;

public interface ListPaymentsPort {

    PageResult<Payment> execute(ListPaymentsQuery query);
}
