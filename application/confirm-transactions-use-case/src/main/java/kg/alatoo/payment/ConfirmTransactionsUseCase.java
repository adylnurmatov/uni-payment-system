package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.BlockchainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.models.RefundStatus;
import kg.alatoo.payment.models.TransactionStatus;
import kg.alatoo.payment.models.TreasuryTransfer;
import kg.alatoo.payment.models.TreasuryTransferStatus;
import kg.alatoo.payment.services.RefundCalculator;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

/**
 * Отслеживает подтверждение отправленных транзакций (вызывается планировщиком раз в ~15 секунд):
 * <ul>
 *     <li>SUCCESS → платёж CONFIRMED, возврат COMPLETED (+ платёж PARTIALLY_REFUNDED / REFUNDED), раздача COMPLETED;</li>
 *     <li>FAILED (tx reverted) → FAILED;</li>
 *     <li>нет receipt дольше таймаута → FAILED с кодом CONFIRMATION_TIMEOUT.</li>
 * </ul>
 * Каждая запись обрабатывается отдельно: ошибка RPC по одной транзакции не мешает остальным,
 * она просто будет проверена в следующем проходе.
 */
@Slf4j
@UseCase
public class ConfirmTransactionsUseCase implements ConfirmTransactionsPort {
    private final PaymentRepositoryPort paymentRepository;
    private final PaymentTransitions paymentTransitions;
    private final RefundRepositoryPort refundRepository;
    private final TreasuryTransferRepositoryPort transferRepository;
    private final BlockchainGateway blockchainGateway;
    private final ConfirmationSettings settings;
    private final Clock clock;

    public ConfirmTransactionsUseCase(PaymentRepositoryPort paymentRepository,
                                      PaymentTransitions paymentTransitions,
                                      RefundRepositoryPort refundRepository,
                                      TreasuryTransferRepositoryPort transferRepository,
                                      BlockchainGateway blockchainGateway,
                                      ConfirmationSettings settings,
                                      Clock clock) {
        this.paymentRepository = paymentRepository;
        this.paymentTransitions = paymentTransitions;
        this.refundRepository = refundRepository;
        this.transferRepository = transferRepository;
        this.blockchainGateway = blockchainGateway;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public ConfirmationReport execute() {
        Counter counter = new Counter();
        for (Payment payment : paymentRepository.findByStatus(PaymentStatus.PENDING)) {
            safely(counter, "payment " + payment.id(), () -> checkPayment(payment, counter));
        }
        for (Refund refund : refundRepository.findByStatus(RefundStatus.PENDING)) {
            safely(counter, "refund " + refund.id(), () -> checkRefund(refund, counter));
        }
        for (TreasuryTransfer transfer : transferRepository.findByStatus(TreasuryTransferStatus.PENDING)) {
            safely(counter, "treasury transfer " + transfer.id(), () -> checkTransfer(transfer, counter));
        }
        ConfirmationReport report = counter.toReport();
        if (!report.isEmpty()) {
            log.info("Confirmation run: {}", report);
        }
        return report;
    }

    private void checkPayment(Payment payment, Counter counter) {
        switch (statusOf(payment.txHash(), payment.submittedAt())) {
            case SUCCESS -> {
                paymentTransitions.moveTo(payment, PaymentStatus.CONFIRMED, "Transaction confirmed in block");
                counter.confirmed++;
            }
            case FAILED -> {
                paymentTransitions.fail(payment, ErrorCode.BLOCKCHAIN_ERROR, "Transaction " + payment.txHash() + " reverted");
                counter.failed++;
            }
            case TIMEOUT -> {
                paymentTransitions.fail(payment, ErrorCode.CONFIRMATION_TIMEOUT, timeoutMessage(payment.txHash()));
                counter.failed++;
            }
            case PENDING -> counter.pending++;
        }
    }

    private void checkRefund(Refund refund, Counter counter) {
        switch (statusOf(refund.txHash(), refund.submittedAt())) {
            case SUCCESS -> {
                refundRepository.save(refund.complete(clock.instant()));
                log.info("[payment={}] refund {} -> COMPLETED", refund.paymentId(), refund.id());
                updatePaymentAfterRefund(refund);
                counter.confirmed++;
            }
            case FAILED -> {
                failRefund(refund, ErrorCode.BLOCKCHAIN_ERROR, "Transaction " + refund.txHash() + " reverted");
                counter.failed++;
            }
            case TIMEOUT -> {
                failRefund(refund, ErrorCode.CONFIRMATION_TIMEOUT, timeoutMessage(refund.txHash()));
                counter.failed++;
            }
            case PENDING -> counter.pending++;
        }
    }

    private void updatePaymentAfterRefund(Refund refund) {
        Payment payment = paymentRepository.findById(refund.paymentId()).orElseThrow();
        PaymentStatus target = RefundCalculator.statusAfterRefunds(payment, refundRepository.findByPaymentId(payment.id()));
        paymentTransitions.moveTo(payment, target,
                "Refund " + refund.id() + " of " + refund.amount().toPlainString() + " ETH completed");
    }

    private void failRefund(Refund refund, ErrorCode code, String message) {
        refundRepository.save(refund.fail(code, message, clock.instant()));
        log.warn("[payment={}] refund {} -> FAILED: {} {}", refund.paymentId(), refund.id(), code, message);
    }

    private void checkTransfer(TreasuryTransfer transfer, Counter counter) {
        switch (statusOf(transfer.txHash(), transfer.createdAt())) {
            case SUCCESS -> {
                transferRepository.save(transfer.withStatus(TreasuryTransferStatus.COMPLETED));
                log.info("Treasury transfer {} -> COMPLETED", transfer.id());
                counter.confirmed++;
            }
            case FAILED, TIMEOUT -> {
                transferRepository.save(transfer.withStatus(TreasuryTransferStatus.FAILED));
                log.warn("Treasury transfer {} -> FAILED (tx {})", transfer.id(), transfer.txHash());
                counter.failed++;
            }
            case PENDING -> counter.pending++;
        }
    }

    /** Статус транзакции с учётом таймаута. */
    private Outcome statusOf(String txHash, Instant submittedAt) {
        TransactionStatus status = txHash == null ? TransactionStatus.PENDING : blockchainGateway.getTransactionStatus(txHash);
        return switch (status) {
            case SUCCESS -> Outcome.SUCCESS;
            case FAILED -> Outcome.FAILED;
            case PENDING -> isTimedOut(submittedAt) ? Outcome.TIMEOUT : Outcome.PENDING;
        };
    }

    private boolean isTimedOut(Instant submittedAt) {
        return submittedAt != null && clock.instant().isAfter(submittedAt.plus(settings.timeout()));
    }

    private String timeoutMessage(String txHash) {
        return "No receipt for transaction " + txHash + " within " + settings.timeout().toMinutes() + " min";
    }

    private void safely(Counter counter, String what, Runnable action) {
        try {
            action.run();
        } catch (BlockchainException e) {
            log.warn("Cannot check {} now, will retry: {}", what, e.getMessage());
            counter.errors++;
        } catch (RuntimeException e) {
            log.error("Failed to process {}", what, e);
            counter.errors++;
        }
    }

    private enum Outcome { SUCCESS, FAILED, TIMEOUT, PENDING }

    private static final class Counter {
        int confirmed;
        int failed;
        int pending;
        int errors;

        ConfirmationReport toReport() {
            return new ConfirmationReport(confirmed, failed, pending, errors);
        }
    }
}
