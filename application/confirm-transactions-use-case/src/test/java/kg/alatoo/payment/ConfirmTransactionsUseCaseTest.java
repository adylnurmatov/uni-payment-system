package kg.alatoo.payment;

import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.fake.FakeBlockchainGateway;
import kg.alatoo.payment.fixtures.InMemoryPaymentRepository;
import kg.alatoo.payment.fixtures.InMemoryRefundRepository;
import kg.alatoo.payment.fixtures.InMemoryTreasuryTransferRepository;
import kg.alatoo.payment.fixtures.MutableClock;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.PaymentStatusChange;
import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.models.RefundStatus;
import kg.alatoo.payment.models.TransactionStatus;
import kg.alatoo.payment.models.TreasuryTransfer;
import kg.alatoo.payment.models.TreasuryTransferStatus;
import kg.alatoo.payment.services.PaymentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmTransactionsUseCaseTest {
    private static final Duration BLOCK_DELAY = Duration.ofSeconds(20);
    private static final Duration TIMEOUT = Duration.ofMinutes(10);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private FakeBlockchainGateway blockchain;
    private InMemoryPaymentRepository paymentRepository;
    private InMemoryRefundRepository refundRepository;
    private InMemoryTreasuryTransferRepository transferRepository;
    private ConfirmTransactionsUseCase useCase;

    @BeforeEach
    void setUp() {
        blockchain = new FakeBlockchainGateway(new BigDecimal("0.000021"), BLOCK_DELAY, clock);
        paymentRepository = new InMemoryPaymentRepository();
        refundRepository = new InMemoryRefundRepository();
        transferRepository = new InMemoryTreasuryTransferRepository();
        PaymentTransitions transitions = new PaymentTransitions(new PaymentStateMachine(), paymentRepository, clock);
        useCase = new ConfirmTransactionsUseCase(paymentRepository, transitions, refundRepository, transferRepository,
                blockchain, new ConfirmationSettings(TIMEOUT), clock);
    }

    @Test
    void pendingPaymentIsConfirmedAfterBlock() {
        Payment payment = pendingPayment(sendTx());

        ConfirmationReport early = useCase.execute();
        assertThat(early.stillPending()).isEqualTo(1);
        assertThat(status(payment)).isEqualTo(PaymentStatus.PENDING);

        clock.advance(BLOCK_DELAY);
        ConfirmationReport later = useCase.execute();

        assertThat(later.confirmed()).isEqualTo(1);
        assertThat(status(payment)).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(paymentRepository.findHistory(payment.id()))
                .extracting(PaymentStatusChange::toStatus)
                .endsWith(PaymentStatus.PENDING, PaymentStatus.CONFIRMED);
    }

    @Test
    void revertedTransactionFailsPayment() {
        String txHash = sendTx();
        Payment payment = pendingPayment(txHash);
        blockchain.forceStatus(txHash, TransactionStatus.FAILED);

        useCase.execute();

        Payment stored = paymentRepository.findById(payment.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(stored.failureCode()).isEqualTo(ErrorCode.BLOCKCHAIN_ERROR);
    }

    @Test
    void noReceiptForTooLongIsConfirmationTimeout() {
        Payment payment = pendingPayment("0xunknown");

        clock.advance(TIMEOUT.minusSeconds(1));
        useCase.execute();
        assertThat(status(payment)).isEqualTo(PaymentStatus.PENDING);

        clock.advance(Duration.ofSeconds(2));
        useCase.execute();
        Payment stored = paymentRepository.findById(payment.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(stored.failureCode()).isEqualTo(ErrorCode.CONFIRMATION_TIMEOUT);
    }

    @Test
    void completedRefundsMovePaymentToPartiallyRefundedThenRefunded() {
        Payment payment = savePayment(PaymentStatus.CONFIRMED, "0xpaid");
        Refund first = pendingRefund(payment, "0.04", sendTx());
        clock.advance(BLOCK_DELAY);
        useCase.execute();

        assertThat(refundRepository.findById(first.id()).orElseThrow().status()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(status(payment)).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);

        pendingRefund(payment, "0.06", sendTx());
        clock.advance(BLOCK_DELAY);
        useCase.execute();

        assertThat(status(payment)).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void timedOutRefundFailsWithoutChangingPayment() {
        Payment payment = savePayment(PaymentStatus.CONFIRMED, "0xpaid");
        Refund refund = pendingRefund(payment, "0.1", "0xlost");

        clock.advance(TIMEOUT.plusSeconds(1));
        useCase.execute();

        Refund stored = refundRepository.findById(refund.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(RefundStatus.FAILED);
        assertThat(stored.failureCode()).isEqualTo(ErrorCode.CONFIRMATION_TIMEOUT);
        assertThat(status(payment)).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void treasuryTransferIsCompleted() {
        TreasuryTransfer transfer = transferRepository.save(new TreasuryTransfer(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("0.05"), sendTx(), TreasuryTransferStatus.PENDING, clock.instant()));

        clock.advance(BLOCK_DELAY);
        useCase.execute();

        assertThat(transferRepository.findById(transfer.id()).orElseThrow().status())
                .isEqualTo(TreasuryTransferStatus.COMPLETED);
    }

    private String sendTx() {
        GeneratedKeyPair from = blockchain.generateKeyPair();
        blockchain.credit(from.address(), BigDecimal.ONE);
        return blockchain.sendTransfer(from.privateKeyHex(), "0x000000000000000000000000000000000000beef",
                new BigDecimal("0.1"));
    }

    private Payment pendingPayment(String txHash) {
        return savePayment(PaymentStatus.PENDING, txHash);
    }

    private Payment savePayment(PaymentStatus status, String txHash) {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), "ext-1", new BigDecimal("0.1"),
                        UUID.randomUUID().toString(), "hash", clock.instant())
                .withStatus(status)
                .withTxHash(txHash)
                .withSubmittedAt(clock.instant());
        return paymentRepository.saveWithHistory(payment,
                new PaymentStatusChange(null, payment.id(), null, status, clock.instant(), "test setup"));
    }

    private Refund pendingRefund(Payment payment, String amount, String txHash) {
        return refundRepository.save(Refund.create(UUID.randomUUID(), payment.id(), new BigDecimal(amount),
                UUID.randomUUID().toString(), "hash", clock.instant()).markPending(txHash, clock.instant()));
    }

    private PaymentStatus status(Payment payment) {
        return paymentRepository.findById(payment.id()).orElseThrow().status();
    }
}
