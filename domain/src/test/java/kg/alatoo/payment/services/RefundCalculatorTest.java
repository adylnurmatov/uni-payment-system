package kg.alatoo.payment.services;

import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.exceptions.InvalidStateException;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.Refund;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundCalculatorTest {
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), "ext-1",
            new BigDecimal("1.0"), "key", "hash", now).withStatus(PaymentStatus.CONFIRMED);

    @Test
    void remainingIgnoresFailedRefunds() {
        List<Refund> refunds = List.of(
                pending("0.3"),
                pending("0.2").fail(ErrorCode.INSUFFICIENT_FUNDS, "no money", now),
                pending("0.1").complete(now));

        assertThat(RefundCalculator.remaining(payment, refunds)).isEqualByComparingTo("0.6");
    }

    @Test
    void paymentIsPartiallyRefundedUntilCompletedSumReachesAmount() {
        List<Refund> partial = List.of(pending("0.4").complete(now), pending("0.6"));
        List<Refund> full = List.of(pending("0.4").complete(now), pending("0.6").complete(now));

        assertThat(RefundCalculator.statusAfterRefunds(payment, partial)).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(RefundCalculator.statusAfterRefunds(payment, full)).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void onlyConfirmedOrPartiallyRefundedPaymentsAreRefundable() {
        assertThat(RefundCalculator.isRefundable(payment)).isTrue();
        assertThat(RefundCalculator.isRefundable(payment.withStatus(PaymentStatus.PARTIALLY_REFUNDED))).isTrue();
        assertThat(RefundCalculator.isRefundable(payment.withStatus(PaymentStatus.PENDING))).isFalse();
        assertThat(RefundCalculator.isRefundable(payment.withStatus(PaymentStatus.REFUNDED))).isFalse();
    }

    @Test
    void completedRefundCannotFail() {
        Refund completed = pending("0.1").complete(now);

        assertThatThrownBy(() -> completed.fail(ErrorCode.CONFIRMATION_TIMEOUT, "late", now))
                .isInstanceOf(InvalidStateException.class);
    }

    private Refund pending(String amount) {
        return Refund.create(UUID.randomUUID(), payment.id(), new BigDecimal(amount), UUID.randomUUID().toString(),
                "hash", now).markPending("0xabc", now);
    }
}
