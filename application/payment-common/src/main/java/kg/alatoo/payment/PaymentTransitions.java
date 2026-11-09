package kg.alatoo.payment;

import kg.alatoo.payment.annotations.ApplicationService;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.services.PaymentStateMachine;
import kg.alatoo.payment.services.PaymentStateMachine.PaymentTransition;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

/**
 * Связка «state machine + сохранение»: каждый переход статуса проверяется
 * {@link PaymentStateMachine} и сохраняется вместе с записью в payment_status_history.
 * Все use case меняют статус платежа только через этот класс.
 */
@Slf4j
@ApplicationService
public class PaymentTransitions {
    private final PaymentStateMachine stateMachine;
    private final PaymentRepositoryPort paymentRepository;
    private final Clock clock;

    public PaymentTransitions(PaymentStateMachine stateMachine,
                              PaymentRepositoryPort paymentRepository,
                              Clock clock) {
        this.stateMachine = stateMachine;
        this.paymentRepository = paymentRepository;
        this.clock = clock;
    }

    /** Сохранить новый платёж (статус CREATED) и первую запись истории. */
    public Payment create(Payment payment) {
        PaymentTransition transition = stateMachine.start(payment, clock.instant());
        Payment saved = paymentRepository.saveWithHistory(transition.payment(), transition.change());
        log.info("[payment={}] created: merchant={}, client={}, amount={} ETH",
                saved.id(), saved.merchantId(), saved.clientExternalId(), saved.amount().toPlainString());
        return saved;
    }

    public Payment moveTo(Payment payment, PaymentStatus to, String note) {
        PaymentTransition transition = stateMachine.transition(payment, to, note, clock.instant());
        Payment saved = paymentRepository.saveWithHistory(transition.payment(), transition.change());
        log.info("[payment={}] {} -> {}{}", saved.id(), payment.status(), to, note == null ? "" : " (" + note + ")");
        return saved;
    }

    public Payment fail(Payment payment, ErrorCode code, String message) {
        Payment withFailure = payment.withFailureCode(code).withFailureMessage(message);
        PaymentTransition transition = stateMachine.transition(withFailure, PaymentStatus.FAILED,
                code + ": " + message, clock.instant());
        Payment saved = paymentRepository.saveWithHistory(transition.payment(), transition.change());
        log.warn("[payment={}] {} -> FAILED: {} {}", saved.id(), payment.status(), code, message);
        return saved;
    }
}
