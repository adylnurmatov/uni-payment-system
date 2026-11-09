package kg.alatoo.payment;

import kg.alatoo.payment.models.Payment;

/** Строка таблицы платежей: платёж + имя мерчанта. */
public record PaymentRow(Payment payment, String merchantName) {
}
