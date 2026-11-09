package kg.alatoo.payment.services;

import kg.alatoo.payment.exceptions.InvalidStateException;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {
    private final PaymentStateMachine stateMachine = new PaymentStateMachine();
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void happyPathGoesFromCreatedToConfirmed() {
        Payment payment = newPayment();
        for (PaymentStatus next : new PaymentStatus[]{
                PaymentStatus.CLIENT_VERIFIED, PaymentStatus.CLIENT_DATA_RECEIVED,
                PaymentStatus.BALANCE_CHECKED, PaymentStatus.PENDING, PaymentStatus.CONFIRMED}) {
            payment = stateMachine.transition(payment, next, null, now).payment();
        }
        assertThat(payment.status()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void transitionProducesHistoryRecord() {
        Payment payment = newPayment();

        var transition = stateMachine.transition(payment, PaymentStatus.CLIENT_VERIFIED, "ok", now);

        assertThat(transition.change().paymentId()).isEqualTo(payment.id());
        assertThat(transition.change().fromStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(transition.change().toStatus()).isEqualTo(PaymentStatus.CLIENT_VERIFIED);
        assertThat(transition.change().note()).isEqualTo("ok");
    }

    @Test
    void startCreatesInitialHistoryRecord() {
        var transition = stateMachine.start(newPayment(), now);

        assertThat(transition.change().fromStatus()).isNull();
        assertThat(transition.change().toStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @ParameterizedTest
    @CsvSource({
            "CREATED, FAILED, true",
            "BALANCE_CHECKED, FAILED, true",
            "PENDING, FAILED, true",
            "CONFIRMED, FAILED, false",
            "CONFIRMED, REFUNDED, true",
            "CONFIRMED, PARTIALLY_REFUNDED, true",
            "PARTIALLY_REFUNDED, REFUNDED, true",
            "CREATED, PENDING, false",
            "FAILED, CREATED, false",
            "REFUNDED, PARTIALLY_REFUNDED, false",
            "PENDING, REFUNDED, false"
    })
    void checksAllowedTransitions(PaymentStatus from, PaymentStatus to, boolean allowed) {
        assertThat(stateMachine.canTransition(from, to)).isEqualTo(allowed);
    }

    @Test
    void forbiddenTransitionThrowsInvalidState() {
        Payment payment = newPayment();

        assertThatThrownBy(() -> stateMachine.transition(payment, PaymentStatus.CONFIRMED, null, now))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("CREATED")
                .hasMessageContaining("CONFIRMED");
    }

    private Payment newPayment() {
        return Payment.create(UUID.randomUUID(), UUID.randomUUID(), "ext-1",
                new BigDecimal("0.001"), "key-1", "hash", now);
    }
}
