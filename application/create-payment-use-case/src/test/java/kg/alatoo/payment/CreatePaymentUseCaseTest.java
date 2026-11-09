package kg.alatoo.payment;

import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.exceptions.PaymentFailedException;
import kg.alatoo.payment.fake.FakeBlockchainGateway;
import kg.alatoo.payment.fixtures.InMemoryClientRepository;
import kg.alatoo.payment.fixtures.InMemoryPaymentRepository;
import kg.alatoo.payment.fixtures.InMemoryWalletRepository;
import kg.alatoo.payment.fixtures.ReversiblePrivateKeyCipher;
import kg.alatoo.payment.fixtures.StubClientDataProvider;
import kg.alatoo.payment.models.Client;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.PaymentStatusChange;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.models.WalletType;
import kg.alatoo.payment.services.PaymentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreatePaymentUseCaseTest {
    private static final BigDecimal FEE = new BigDecimal("0.000021");

    private final Clock clock = Clock.systemUTC();
    private FakeBlockchainGateway blockchain;
    private InMemoryPaymentRepository paymentRepository;
    private InMemoryClientRepository clientRepository;
    private StubClientDataProvider abc;
    private WalletService walletService;
    private CreatePaymentUseCase useCase;

    private Merchant merchant;
    private Wallet merchantWallet;
    private Wallet clientWallet;

    @BeforeEach
    void setUp() {
        blockchain = new FakeBlockchainGateway(FEE, Duration.ofSeconds(20), clock);
        paymentRepository = new InMemoryPaymentRepository();
        clientRepository = new InMemoryClientRepository();
        abc = new StubClientDataProvider();
        walletService = new WalletService(blockchain, new ReversiblePrivateKeyCipher(), new InMemoryWalletRepository(), clock);
        PaymentTransitions transitions = new PaymentTransitions(new PaymentStateMachine(), paymentRepository, clock);
        useCase = new CreatePaymentUseCase(paymentRepository, transitions, clientRepository, abc, walletService, clock);

        merchantWallet = walletService.createWallet(WalletType.MERCHANT);
        merchant = new Merchant(UUID.randomUUID(), "Shop", "hash", merchantWallet.id(), clock.instant());

        clientWallet = walletService.createWallet(WalletType.CLIENT);
        clientRepository.save(new Client(UUID.randomUUID(), "ext-42", clientWallet.id(), clock.instant()));
        blockchain.credit(clientWallet.address(), new BigDecimal("1"));
    }

    @Test
    void successfulPaymentIsPendingAndMovesMoney() {
        Payment payment = useCase.execute(command("ext-42", "0.1", "key-1"));

        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.txHash()).startsWith("0x");
        assertThat(payment.submittedAt()).isNotNull();
        assertThat(blockchain.getBalance(merchantWallet.address())).isEqualByComparingTo("0.1");
        assertThat(blockchain.getBalance(clientWallet.address()))
                .isEqualByComparingTo(new BigDecimal("0.9").subtract(FEE));
    }

    @Test
    void everyTransitionIsWrittenToHistory() {
        Payment payment = useCase.execute(command("ext-42", "0.1", "key-1"));

        assertThat(paymentRepository.findHistory(payment.id()))
                .extracting(PaymentStatusChange::toStatus)
                .containsExactly(PaymentStatus.CREATED, PaymentStatus.CLIENT_VERIFIED,
                        PaymentStatus.CLIENT_DATA_RECEIVED, PaymentStatus.BALANCE_CHECKED, PaymentStatus.PENDING);
    }

    @Test
    void sameKeyAndSameBodyReturnsSamePaymentWithoutSecondCharge() {
        Payment first = useCase.execute(command("ext-42", "0.1", "key-1"));
        Payment second = useCase.execute(command("ext-42", "0.100", "key-1"));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.txHash()).isEqualTo(first.txHash());
        assertThat(blockchain.getBalance(merchantWallet.address())).isEqualByComparingTo("0.1");
    }

    @Test
    void sameKeyWithDifferentBodyIsConflict() {
        useCase.execute(command("ext-42", "0.1", "key-1"));

        assertThatThrownBy(() -> useCase.execute(command("ext-42", "0.2", "key-1")))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @Test
    void insufficientFundsFailsPaymentAndSaysHowMuchIsMissing() {
        assertThatThrownBy(() -> useCase.execute(command("ext-42", "1", "key-1")))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("Missing " + FEE.toPlainString())
                .satisfies(e -> assertFailed((PaymentFailedException) e, ErrorCode.INSUFFICIENT_FUNDS));
        assertThat(blockchain.getBalance(merchantWallet.address())).isEqualByComparingTo("0");
    }

    @Test
    void failedPaymentIsReplayedWithSameError() {
        PaymentFailedException first = catchFailure(() -> useCase.execute(command("ext-42", "5", "key-1")));
        PaymentFailedException second = catchFailure(() -> useCase.execute(command("ext-42", "5", "key-1")));

        assertThat(second.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(second.getCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
    }

    @Test
    void unknownClientFails() {
        PaymentFailedException e = catchFailure(() -> useCase.execute(command("nobody", "0.1", "key-1")));
        assertFailed(e, ErrorCode.CLIENT_NOT_FOUND);
    }

    @Test
    void clientWithoutWalletFails() {
        clientRepository.save(new Client(UUID.randomUUID(), "no-wallet", null, clock.instant()));

        PaymentFailedException e = catchFailure(() -> useCase.execute(command("no-wallet", "0.1", "key-1")));
        assertFailed(e, ErrorCode.WALLET_NOT_FOUND);
    }

    @Test
    void blockedClientFails() {
        abc.block("ext-42");

        PaymentFailedException e = catchFailure(() -> useCase.execute(command("ext-42", "0.1", "key-1")));
        assertFailed(e, ErrorCode.CLIENT_BLOCKED);
        assertThat(paymentRepository.findHistory(e.getPaymentId()))
                .extracting(PaymentStatusChange::toStatus)
                .containsExactly(PaymentStatus.CREATED, PaymentStatus.CLIENT_VERIFIED, PaymentStatus.FAILED);
    }

    @Test
    void unavailableAbcFails() {
        abc.setAvailable(false);

        PaymentFailedException e = catchFailure(() -> useCase.execute(command("ext-42", "0.1", "key-1")));
        assertFailed(e, ErrorCode.CLIENT_DATA_UNAVAILABLE);
    }

    @Test
    void invalidAmountIsRejectedWithoutCreatingPayment() {
        assertThatThrownBy(() -> useCase.execute(command("ext-42", "-1", "key-1")))
                .isInstanceOf(DomainException.class)
                .isNotInstanceOf(PaymentFailedException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_AMOUNT);
        assertThat(paymentRepository.findByMerchantIdAndIdempotencyKey(merchant.id(), "key-1")).isEmpty();
    }

    @Test
    void missingIdempotencyKeyIsValidationError() {
        assertThatThrownBy(() -> useCase.execute(command("ext-42", "0.1", " ")))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private void assertFailed(PaymentFailedException e, ErrorCode code) {
        assertThat(e.getCode()).isEqualTo(code);
        Payment stored = paymentRepository.findById(e.getPaymentId()).orElseThrow();
        assertThat(stored.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(stored.failureCode()).isEqualTo(code);
    }

    private static PaymentFailedException catchFailure(Runnable action) {
        try {
            action.run();
        } catch (PaymentFailedException e) {
            return e;
        }
        throw new AssertionError("Expected PaymentFailedException");
    }

    private CreatePaymentCommand command(String clientId, String amount, String key) {
        return new CreatePaymentCommand(merchant, clientId, new BigDecimal(amount), key);
    }
}
