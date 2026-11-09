package kg.alatoo.payment.services;

import kg.alatoo.payment.exceptions.InvalidStateException;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.PaymentStatusChange;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static kg.alatoo.payment.models.PaymentStatus.*;

/**
 * Единственное место, где описаны допустимые переходы статусов платежа.
 *
 * <pre>
 * CREATED → CLIENT_VERIFIED → CLIENT_DATA_RECEIVED → BALANCE_CHECKED → PENDING → CONFIRMED
 *    любой шаг до PENDING ──→ FAILED
 *    PENDING ──(tx reverted / timeout)──→ FAILED
 *    CONFIRMED → PARTIALLY_REFUNDED → REFUNDED
 *    CONFIRMED → REFUNDED
 * </pre>
 *
 * Класс чистый (без БД): он только проверяет переход и возвращает новый платёж + запись аудита.
 * Сохранение делает application-слой (PaymentTransitions).
 */
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED.put(CREATED, EnumSet.of(CLIENT_VERIFIED, FAILED));
        ALLOWED.put(CLIENT_VERIFIED, EnumSet.of(CLIENT_DATA_RECEIVED, FAILED));
        ALLOWED.put(CLIENT_DATA_RECEIVED, EnumSet.of(BALANCE_CHECKED, FAILED));
        ALLOWED.put(BALANCE_CHECKED, EnumSet.of(PENDING, FAILED));
        ALLOWED.put(PENDING, EnumSet.of(CONFIRMED, FAILED));
        ALLOWED.put(CONFIRMED, EnumSet.of(PARTIALLY_REFUNDED, REFUNDED));
        ALLOWED.put(PARTIALLY_REFUNDED, EnumSet.of(PARTIALLY_REFUNDED, REFUNDED));
        ALLOWED.put(REFUNDED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED.put(FAILED, EnumSet.noneOf(PaymentStatus.class));
    }

    public boolean canTransition(PaymentStatus from, PaymentStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    /** Первая запись истории для только что созданного платежа (null → CREATED). */
    public PaymentTransition start(Payment payment, Instant now) {
        if (payment.status() != CREATED) {
            throw new InvalidStateException("New payment must start in CREATED, but was " + payment.status());
        }
        PaymentStatusChange change = new PaymentStatusChange(null, payment.id(), null, CREATED, now, "Payment created");
        return new PaymentTransition(payment, change);
    }

    /**
     * Переводит платёж в новый статус.
     *
     * @throws InvalidStateException если переход запрещён
     */
    public PaymentTransition transition(Payment payment, PaymentStatus to, String note, Instant now) {
        PaymentStatus from = payment.status();
        if (!canTransition(from, to)) {
            throw new InvalidStateException(
                    "Payment " + payment.id() + " cannot move from " + from + " to " + to);
        }
        Payment updated = payment.withStatus(to).withUpdatedAt(now);
        PaymentStatusChange change = new PaymentStatusChange(null, payment.id(), from, to, now, note);
        return new PaymentTransition(updated, change);
    }

    /** Результат перехода: новое состояние платежа и запись для payment_status_history. */
    public record PaymentTransition(Payment payment, PaymentStatusChange change) {
    }
}
