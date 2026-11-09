package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.exceptions.InvalidStateException;
import kg.alatoo.payment.exceptions.RefundFailedException;
import kg.alatoo.payment.models.Client;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.services.AmountValidator;
import kg.alatoo.payment.services.RefundCalculator;
import kg.alatoo.payment.utils.Hashing;
import kg.alatoo.payment.utils.V7UUIDGenerator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;

/**
 * Возврат по платежу (RefundService из ТЗ).
 * <ol>
 *     <li>идемпотентность по (payment_id, Idempotency-Key);</li>
 *     <li>возврат разрешён только для CONFIRMED / PARTIALLY_REFUNDED;</li>
 *     <li>сумма всех не-FAILED возвратов ≤ сумма платежа (по умолчанию возвращается весь остаток);</li>
 *     <li>новая транзакция с кошелька мерчанта на кошелёк клиента, комиссию платит мерчант.</li>
 * </ol>
 * Проверка остатка и отправка выполняются под блокировкой адреса мерчанта: так два параллельных
 * возврата не превысят сумму платежа и не получат одинаковый nonce.
 * Статус платежа (PARTIALLY_REFUNDED / REFUNDED) меняется, когда возврат подтвердится в блоке.
 */
@Slf4j
@UseCase
public class RefundPaymentUseCase implements RefundPaymentPort {
    private final PaymentRepositoryPort paymentRepository;
    private final RefundRepositoryPort refundRepository;
    private final ClientRepositoryPort clientRepository;
    private final WalletService walletService;
    private final Clock clock;

    public RefundPaymentUseCase(PaymentRepositoryPort paymentRepository,
                                RefundRepositoryPort refundRepository,
                                ClientRepositoryPort clientRepository,
                                WalletService walletService,
                                Clock clock) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.clientRepository = clientRepository;
        this.walletService = walletService;
        this.clock = clock;
    }

    @Override
    public Refund execute(RefundPaymentCommand command) {
        Merchant merchant = command.merchant();
        String idempotencyKey = IdempotencyKeys.require(command.idempotencyKey());
        BigDecimal requested = command.amount() == null ? null : AmountValidator.requireValid(command.amount());

        Payment payment = paymentRepository.findById(command.paymentId())
                .filter(p -> p.merchantId().equals(merchant.id()))
                .orElseThrow(() -> new DomainException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment " + command.paymentId() + " not found"));
        String requestHash = Hashing.sha256Hex(payment.id() + "|"
                + (requested == null ? "REMAINING" : requested.toPlainString()));

        Optional<Refund> existing = refundRepository.findByPaymentIdAndIdempotencyKey(payment.id(), idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), requestHash);
        }

        if (!RefundCalculator.isRefundable(payment)) {
            throw new InvalidStateException("Refund is allowed only for CONFIRMED or PARTIALLY_REFUNDED payments, "
                    + "but payment " + payment.id() + " is " + payment.status());
        }

        Wallet clientWallet = clientWalletOf(payment);
        Wallet merchantWallet = walletService.getWallet(merchant.walletId());

        return walletService.withSendLock(merchantWallet, () -> {
            // Повторная проверка под блокировкой: параллельный запрос с тем же ключом мог успеть первым
            Optional<Refund> concurrent = refundRepository.findByPaymentIdAndIdempotencyKey(payment.id(), idempotencyKey);
            if (concurrent.isPresent()) {
                return replay(concurrent.get(), requestHash);
            }

            BigDecimal remaining = RefundCalculator.remaining(payment, refundRepository.findByPaymentId(payment.id()));
            BigDecimal amount = requested == null ? remaining : requested;
            if (remaining.signum() == 0 || amount.compareTo(remaining) > 0) {
                throw new DomainException(ErrorCode.REFUND_EXCEEDS_PAYMENT,
                        "Requested refund " + amount.toPlainString() + " ETH, but only "
                                + remaining.toPlainString() + " ETH of payment " + payment.id() + " can be refunded");
            }

            Refund refund = refundRepository.save(Refund.create(V7UUIDGenerator.generate(), payment.id(), amount,
                    idempotencyKey, requestHash, clock.instant()));
            log.info("[payment={}] refund {} created: {} ETH", payment.id(), refund.id(), amount.toPlainString());

            try {
                checkMerchantBalance(merchantWallet, clientWallet, amount);
                String txHash = walletService.send(merchantWallet, clientWallet.address(), amount);
                Refund pending = refundRepository.save(refund.markPending(txHash, clock.instant()));
                log.info("[payment={}] refund {} -> PENDING (tx {})", payment.id(), refund.id(), txHash);
                return pending;
            } catch (DomainException e) {
                throw failRefund(refund, e.getCode(), e.getMessage());
            } catch (RuntimeException e) {
                log.error("[payment={}] refund {} unexpected error", payment.id(), refund.id(), e);
                throw failRefund(refund, ErrorCode.INTERNAL_ERROR, "Unexpected error: " + e.getMessage());
            }
        });
    }

    private Wallet clientWalletOf(Payment payment) {
        Client client = clientRepository.findById(payment.clientId())
                .orElseThrow(() -> new DomainException(ErrorCode.CLIENT_NOT_FOUND,
                        "Client of payment " + payment.id() + " not found"));
        if (!client.hasWallet()) {
            throw new DomainException(ErrorCode.WALLET_NOT_FOUND, "Client '" + client.externalId() + "' has no wallet");
        }
        return walletService.getWallet(client.walletId());
    }

    private void checkMerchantBalance(Wallet merchantWallet, Wallet clientWallet, BigDecimal amount) {
        BigDecimal fee = walletService.estimateTransferFee(merchantWallet, clientWallet.address(), amount);
        BigDecimal balance = walletService.getBalance(merchantWallet);
        BigDecimal required = amount.add(fee);
        if (balance.compareTo(required) < 0) {
            throw new DomainException(ErrorCode.INSUFFICIENT_FUNDS,
                    "Merchant balance " + balance.toPlainString() + " ETH is not enough for refund "
                            + amount.toPlainString() + " ETH + network fee " + fee.toPlainString()
                            + " ETH. Missing " + required.subtract(balance).toPlainString() + " ETH");
        }
    }

    private RefundFailedException failRefund(Refund refund, ErrorCode code, String message) {
        Refund failed = refundRepository.save(refund.fail(code, message, clock.instant()));
        log.warn("[payment={}] refund {} -> FAILED: {} {}", failed.paymentId(), failed.id(), code, message);
        return new RefundFailedException(code, message, failed.paymentId(), failed.id());
    }

    private Refund replay(Refund existing, String requestHash) {
        if (!existing.requestHash().equals(requestHash)) {
            throw new DomainException(ErrorCode.IDEMPOTENCY_CONFLICT,
                    "Idempotency-Key was already used with a different request body (refund " + existing.id() + ")");
        }
        log.info("[payment={}] refund {} idempotent replay, status {}", existing.paymentId(), existing.id(), existing.status());
        if (existing.isFailed()) {
            throw new RefundFailedException(existing.failureCode(), existing.failureMessage(),
                    existing.paymentId(), existing.id());
        }
        return existing;
    }
}
