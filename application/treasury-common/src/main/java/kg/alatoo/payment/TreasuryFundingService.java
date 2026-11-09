package kg.alatoo.payment;

import kg.alatoo.payment.annotations.ApplicationService;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.TreasuryTransfer;
import kg.alatoo.payment.models.TreasuryTransferStatus;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.services.AmountValidator;
import kg.alatoo.payment.utils.V7UUIDGenerator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Раздача тестового ETH из казны. Кран (faucet) пополняет только казну,
 * а клиенты и мерчанты получают деньги отсюда.
 */
@Slf4j
@ApplicationService
public class TreasuryFundingService {
    private final WalletService walletService;
    private final TreasuryTransferRepositoryPort transferRepository;
    private final Clock clock;

    public TreasuryFundingService(WalletService walletService,
                                  TreasuryTransferRepositoryPort transferRepository,
                                  Clock clock) {
        this.walletService = walletService;
        this.transferRepository = transferRepository;
        this.clock = clock;
    }

    /** Предварительная проверка: хватит ли в казне на перевод amount (используется до создания мерчанта). */
    public void ensureCanFund(BigDecimal amount) {
        Wallet treasury = walletService.getTreasury();
        checkTreasuryBalance(treasury, AmountValidator.requireValid(amount));
    }

    /** Перевести amount ETH из казны на кошелёк target. Транзакция уходит со статусом PENDING. */
    public TreasuryTransfer fund(Wallet target, BigDecimal amount) {
        BigDecimal validAmount = AmountValidator.requireValid(amount);
        Wallet treasury = walletService.getTreasury();

        return walletService.withSendLock(treasury, () -> {
            checkTreasuryBalance(treasury, validAmount);
            String txHash = walletService.send(treasury, target.address(), validAmount);
            TreasuryTransfer transfer = new TreasuryTransfer(
                    V7UUIDGenerator.generate(), target.id(), validAmount, txHash,
                    TreasuryTransferStatus.PENDING, clock.instant());
            TreasuryTransfer saved = transferRepository.save(transfer);
            log.info("Treasury transfer {}: {} ETH to wallet {} (tx {})",
                    saved.id(), validAmount.toPlainString(), target.id(), txHash);
            return saved;
        });
    }

    private void checkTreasuryBalance(Wallet treasury, BigDecimal amount) {
        BigDecimal fee = walletService.estimateTransferFee();
        BigDecimal balance = walletService.getBalance(treasury);
        BigDecimal required = amount.add(fee);
        if (balance.compareTo(required) < 0) {
            throw new DomainException(ErrorCode.TREASURY_INSUFFICIENT_FUNDS,
                    "Treasury balance " + balance.toPlainString() + " ETH is not enough for "
                            + amount.toPlainString() + " ETH + fee " + fee.toPlainString()
                            + " ETH. Missing " + required.subtract(balance).toPlainString()
                            + " ETH. Top up treasury " + treasury.address() + " from the faucet.");
        }
    }
}
