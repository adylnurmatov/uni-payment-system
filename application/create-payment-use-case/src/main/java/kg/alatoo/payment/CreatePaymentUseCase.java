package kg.alatoo.payment;

import kg.alatoo.payment.annotations.UseCase;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.exceptions.PaymentFailedException;
import kg.alatoo.payment.models.Client;
import kg.alatoo.payment.models.ClientProfile;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.services.AmountValidator;
import kg.alatoo.payment.utils.Hashing;
import kg.alatoo.payment.utils.V7UUIDGenerator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Основной сценарий оплаты:
 * <ol>
 *     <li>идемпотентность: тот же Idempotency-Key + то же тело → тот же результат без повторного списания;</li>
 *     <li>есть ли клиент и его кошелёк → CLIENT_VERIFIED;</li>
 *     <li>данные клиента из «сервиса ABC», BLOCKED → отказ → CLIENT_DATA_RECEIVED;</li>
 *     <li>баланс ≥ сумма + комиссия сети → BALANCE_CHECKED;</li>
 *     <li>отправка транзакции клиент → мерчант → PENDING (подтверждение отслеживает планировщик).</li>
 * </ol>
 * Любая ошибка на шагах 2–5 переводит платёж в FAILED с кодом ошибки.
 */
@Slf4j
@UseCase
public class CreatePaymentUseCase implements CreatePaymentPort {
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentTransitions transitions;
    private final ClientRepositoryPort clientRepository;
    private final ClientDataProvider clientDataProvider;
    private final WalletService walletService;
    private final Clock clock;

    public CreatePaymentUseCase(PaymentRepositoryPort paymentRepository,
                                PaymentTransitions transitions,
                                ClientRepositoryPort clientRepository,
                                ClientDataProvider clientDataProvider,
                                WalletService walletService,
                                Clock clock) {
        this.paymentRepository = paymentRepository;
        this.transitions = transitions;
        this.clientRepository = clientRepository;
        this.clientDataProvider = clientDataProvider;
        this.walletService = walletService;
        this.clock = clock;
    }

    @Override
    public Payment execute(CreatePaymentCommand command) {
        Merchant merchant = command.merchant();
        String idempotencyKey = requireIdempotencyKey(command.idempotencyKey());
        String clientExternalId = requireClientId(command.clientExternalId());
        BigDecimal amount = AmountValidator.requireValid(command.amount());
        String requestHash = Hashing.sha256Hex(clientExternalId + "|" + amount.toPlainString());

        // 1. Идемпотентность: такой запрос уже был?
        Optional<Payment> existing = paymentRepository.findByMerchantIdAndIdempotencyKey(merchant.id(), idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), requestHash);
        }

        Payment payment;
        try {
            payment = transitions.create(Payment.create(V7UUIDGenerator.generate(), merchant.id(),
                    clientExternalId, amount, idempotencyKey, requestHash, clock.instant()));
        } catch (DuplicateIdempotencyKeyException race) {
            // Параллельный запрос с тем же ключом успел первым — отвечаем его результатом.
            Payment winner = paymentRepository.findByMerchantIdAndIdempotencyKey(merchant.id(), idempotencyKey)
                    .orElseThrow(() -> race);
            return replay(winner, requestHash);
        }

        return process(payment, merchant);
    }

    private Payment process(Payment created, Merchant merchant) {
        // Держим актуальное состояние платежа, чтобы при ошибке перевести в FAILED именно его.
        AtomicReference<Payment> current = new AtomicReference<>(created);
        try {
            // 2. Клиент и кошелёк
            Client client = clientRepository.findByExternalId(created.clientExternalId())
                    .orElseThrow(() -> new DomainException(ErrorCode.CLIENT_NOT_FOUND,
                            "Client '" + created.clientExternalId() + "' not found"));
            if (!client.hasWallet()) {
                throw new DomainException(ErrorCode.WALLET_NOT_FOUND,
                        "Client '" + client.externalId() + "' has no wallet");
            }
            Wallet clientWallet = walletService.getWallet(client.walletId());
            current.set(transitions.moveTo(current.get().withClientId(client.id()),
                    PaymentStatus.CLIENT_VERIFIED, "Client and wallet found"));

            // 3. Данные клиента из сервиса ABC
            ClientProfile profile = clientDataProvider.getClientProfile(client.externalId());
            if (profile.isBlocked()) {
                throw new DomainException(ErrorCode.CLIENT_BLOCKED,
                        "Client '" + client.externalId() + "' is blocked by ABC");
            }
            current.set(transitions.moveTo(current.get(), PaymentStatus.CLIENT_DATA_RECEIVED,
                    "ABC: " + profile.fullName() + ", " + profile.country()));

            // 4–5. Проверка баланса и отправка — под блокировкой адреса клиента (nonce + без двойной траты)
            Wallet merchantWallet = walletService.getWallet(merchant.walletId());
            return walletService.withSendLock(clientWallet, () -> {
                checkBalance(current, clientWallet);
                String txHash = walletService.send(clientWallet, merchantWallet.address(), current.get().amount());
                current.set(transitions.moveTo(
                        current.get().withTxHash(txHash).withSubmittedAt(clock.instant()),
                        PaymentStatus.PENDING, "Transaction sent: " + txHash));
                return current.get();
            });
        } catch (DomainException e) {
            throw failPayment(current.get(), e.getCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("[payment={}] unexpected error", current.get().id(), e);
            throw failPayment(current.get(), ErrorCode.INTERNAL_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

    private void checkBalance(AtomicReference<Payment> current, Wallet clientWallet) {
        Payment payment = current.get();
        BigDecimal fee = walletService.estimateTransferFee();
        BigDecimal balance = walletService.getBalance(clientWallet);
        BigDecimal required = payment.amount().add(fee);
        if (balance.compareTo(required) < 0) {
            throw new DomainException(ErrorCode.INSUFFICIENT_FUNDS,
                    "Client balance " + balance.toPlainString() + " ETH is not enough for amount "
                            + payment.amount().toPlainString() + " ETH + network fee " + fee.toPlainString()
                            + " ETH. Missing " + required.subtract(balance).toPlainString() + " ETH");
        }
        current.set(transitions.moveTo(payment, PaymentStatus.BALANCE_CHECKED,
                "Balance " + balance.toPlainString() + " ETH, fee " + fee.toPlainString() + " ETH"));
    }

    private PaymentFailedException failPayment(Payment payment, ErrorCode code, String message) {
        Payment failed = transitions.fail(payment, code, message);
        return new PaymentFailedException(code, message, failed.id());
    }

    private Payment replay(Payment existing, String requestHash) {
        if (!existing.requestHash().equals(requestHash)) {
            throw new DomainException(ErrorCode.IDEMPOTENCY_CONFLICT,
                    "Idempotency-Key was already used with a different request body (payment " + existing.id() + ")");
        }
        log.info("[payment={}] idempotent replay, status {}", existing.id(), existing.status());
        if (existing.isFailed()) {
            throw new PaymentFailedException(existing.failureCode(), existing.failureMessage(), existing.id());
        }
        return existing;
    }

    private static String requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header is required");
        }
        if (key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR,
                    "Idempotency-Key must be at most " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }
        return key.trim();
    }

    private static String requireClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "clientId is required");
        }
        return clientId.trim();
    }
}
