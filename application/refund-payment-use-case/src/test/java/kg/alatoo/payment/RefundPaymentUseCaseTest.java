package kg.alatoo.payment;

import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.exceptions.RefundFailedException;
import kg.alatoo.payment.fake.FakeBlockchainGateway;
import kg.alatoo.payment.fixtures.InMemoryClientRepository;
import kg.alatoo.payment.fixtures.InMemoryPaymentRepository;
import kg.alatoo.payment.fixtures.InMemoryRefundRepository;
import kg.alatoo.payment.fixtures.InMemoryWalletRepository;
import kg.alatoo.payment.fixtures.ReversiblePrivateKeyCipher;
import kg.alatoo.payment.models.Client;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.models.Payment;
import kg.alatoo.payment.models.PaymentStatus;
import kg.alatoo.payment.models.PaymentStatusChange;
import kg.alatoo.payment.models.Refund;
import kg.alatoo.payment.models.RefundStatus;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.models.WalletType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundPaymentUseCaseTest {
    private static final BigDecimal FEE = new BigDecimal("0.000021");

    private final Clock clock = Clock.systemUTC();
    private FakeBlockchainGateway blockchain;
    private InMemoryPaymentRepository paymentRepository;
    private InMemoryRefundRepository refundRepository;
    private RefundPaymentUseCase useCase;

    private Merchant merchant;
    private Wallet merchantWallet;
    private Wallet clientWallet;
    private Payment payment;

    @BeforeEach
    void setUp() {
        blockchain = new FakeBlockchainGateway(FEE, Duration.ofSeconds(20), clock);
        paymentRepository = new InMemoryPaymentRepository();
        refundRepository = new InMemoryRefundRepository();
        InMemoryClientRepository clientRepository = new InMemoryClientRepository();
        WalletService walletService = new WalletService(blockchain, new ReversiblePrivateKeyCipher(),
                new InMemoryWalletRepository(), clock);
        useCase = new RefundPaymentUseCase(paymentRepository, refundRepository, clientRepository, walletService, clock);

        merchantWallet = walletService.createWallet(WalletType.MERCHANT);
        merchant = new Merchant(UUID.randomUUID(), "Shop", "hash", merchantWallet.id(), clock.instant());
        clientWallet = walletService.createWallet(WalletType.CLIENT);
        Client client = clientRepository.save(new Client(UUID.randomUUID(), "ext-42", clientWallet.id(), clock.instant()));

        // Подтверждённый платёж на 0.1 ETH; у мерчанта 0.1 от платежа + 0.01 на комиссии
        payment = savePayment(client, PaymentStatus.CONFIRMED);
        blockchain.credit(merchantWallet.address(), new BigDecimal("0.11"));
    }

    @Test
    void refundsWholePaymentByDefault() {
        Refund refund = useCase.execute(command(null, "r-1"));

        assertThat(refund.status()).isEqualTo(RefundStatus.PENDING);
        assertThat(refund.amount()).isEqualByComparingTo("0.1");
        assertThat(refund.txHash()).startsWith("0x");
        assertThat(blockchain.getBalance(clientWallet.address())).isEqualByComparingTo("0.1");
        assertThat(blockchain.getBalance(merchantWallet.address()))
                .isEqualByComparingTo(new BigDecimal("0.01").subtract(FEE));
    }

    @Test
    void partialRefundThenRemainingAmount() {
        useCase.execute(command("0.03", "r-1"));
        Refund rest = useCase.execute(command(null, "r-2"));

        assertThat(rest.amount()).isEqualByComparingTo("0.07");
    }

    @Test
    void refundCannotExceedPayment() {
        assertThatThrownBy(() -> useCase.execute(command("0.2", "r-1")))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo(ErrorCode.REFUND_EXCEEDS_PAYMENT);
        assertThat(refundRepository.findByPaymentId(payment.id())).isEmpty();
    }

    @Test
    void pendingRefundsCountTowardsLimit() {
        useCase.execute(command(null, "r-1"));

        assertThatThrownBy(() -> useCase.execute(command("0.01", "r-2")))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo(ErrorCode.REFUND_EXCEEDS_PAYMENT);
    }

    @Test
    void refundIsAllowedOnlyForConfirmedPayments() {
        paymentRepository.saveWithHistory(payment.withStatus(PaymentStatus.PENDING), change(PaymentStatus.PENDING));

        assertThatThrownBy(() -> useCase.execute(command(null, "r-1")))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void merchantWithoutMoneyForFeeGetsFailedRefundThatDoesNotBlockLaterRefunds() {
        // Мерчант вывел почти всё: 0.1 ровно, без запаса на комиссию
        String spender = blockchain.sendTransfer(new ReversiblePrivateKeyCipher().decrypt(merchantWallet.encryptedPrivateKey()),
                "0x000000000000000000000000000000000000dead", new BigDecimal("0.01").subtract(FEE));
        assertThat(spender).isNotBlank();

        RefundFailedException e = catchRefundFailure(() -> useCase.execute(command(null, "r-1")));
        assertThat(e.getCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
        assertThat(refundRepository.findById(e.getRefundId()).orElseThrow().status()).isEqualTo(RefundStatus.FAILED);

        // Мерчант пополнил кошелёк — неудачный возврат не занимает лимит
        blockchain.credit(merchantWallet.address(), new BigDecimal("0.01"));
        assertThat(useCase.execute(command(null, "r-2")).amount()).isEqualByComparingTo("0.1");
    }

    @Test
    void sameKeyReplaysAndDifferentBodyConflicts() {
        Refund first = useCase.execute(command("0.03", "r-1"));
        Refund replay = useCase.execute(command("0.030", "r-1"));

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(refundRepository.findByPaymentId(payment.id())).hasSize(1);
        assertThatThrownBy(() -> useCase.execute(command("0.05", "r-1")))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @Test
    void otherMerchantCannotRefund() {
        Merchant other = new Merchant(UUID.randomUUID(), "Other", "hash2", merchantWallet.id(), clock.instant());

        assertThatThrownBy(() -> useCase.execute(new RefundPaymentCommand(other, payment.id(), null, "r-1")))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    private Payment savePayment(Client client, PaymentStatus status) {
        Payment p = Payment.create(UUID.randomUUID(), merchant.id(), client.externalId(), new BigDecimal("0.1"),
                        "pay-1", "hash", clock.instant())
                .withClientId(client.id())
                .withTxHash("0xpayment")
                .withStatus(status);
        return paymentRepository.saveWithHistory(p, change(status));
    }

    private PaymentStatusChange change(PaymentStatus status) {
        return new PaymentStatusChange(null, payment == null ? null : payment.id(), null, status, clock.instant(), "test");
    }

    private RefundPaymentCommand command(String amount, String key) {
        return new RefundPaymentCommand(merchant, payment.id(), amount == null ? null : new BigDecimal(amount), key);
    }

    private static RefundFailedException catchRefundFailure(Runnable action) {
        try {
            action.run();
        } catch (RefundFailedException e) {
            return e;
        }
        throw new AssertionError("Expected RefundFailedException");
    }
}
