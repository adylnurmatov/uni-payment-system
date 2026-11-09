package kg.alatoo.payment;

import kg.alatoo.payment.annotations.ApplicationService;
import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;
import kg.alatoo.payment.models.Wallet;
import kg.alatoo.payment.models.WalletType;
import kg.alatoo.payment.utils.V7UUIDGenerator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Всё, что связано с кастодиальными кошельками:
 * <ul>
 *     <li>создание кошелька (генерация ключей + шифрование + сохранение);</li>
 *     <li>баланс и оценка комиссии;</li>
 *     <li>отправка денег с кошелька — строго последовательно для одного адреса.</li>
 * </ul>
 *
 * <b>Почему нужна блокировка по адресу (nonce).</b> У каждой транзакции Ethereum есть nonce —
 * порядковый номер транзакции отправителя. Если с одного адреса одновременно отправить две
 * транзакции, обе получат одинаковый nonce и одна из них будет отклонена узлом. Кроме того,
 * «проверка баланса → отправка» должна быть атомарной, иначе два параллельных платежа оба
 * пройдут проверку и потратят одни и те же деньги. Поэтому проверка баланса и отправка
 * выполняются внутри {@link #withSendLock(Wallet, Supplier)}.
 * <p>
 * Блокировка in-memory (ReentrantLock на адрес) — достаточно для одного экземпляра приложения.
 * Для нескольких экземпляров её нужно заменить на блокировку в БД (SELECT ... FOR UPDATE по строке кошелька).
 */
@Slf4j
@ApplicationService
public class WalletService {
    private final BlockchainGateway blockchainGateway;
    private final PrivateKeyCipher privateKeyCipher;
    private final WalletRepositoryPort walletRepository;
    private final Clock clock;
    private final ConcurrentHashMap<String, ReentrantLock> sendLocks = new ConcurrentHashMap<>();

    public WalletService(BlockchainGateway blockchainGateway,
                         PrivateKeyCipher privateKeyCipher,
                         WalletRepositoryPort walletRepository,
                         Clock clock) {
        this.blockchainGateway = blockchainGateway;
        this.privateKeyCipher = privateKeyCipher;
        this.walletRepository = walletRepository;
        this.clock = clock;
    }

    public Wallet createWallet(WalletType type) {
        GeneratedKeyPair keyPair = blockchainGateway.generateKeyPair();
        Wallet wallet = new Wallet(
                V7UUIDGenerator.generate(),
                type,
                keyPair.address(),
                privateKeyCipher.encrypt(keyPair.privateKeyHex()),
                clock.instant()
        );
        Wallet saved = walletRepository.save(wallet);
        log.info("Created {} wallet {} with address {}", type, saved.id(), saved.address());
        return saved;
    }

    public Wallet getWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new DomainException(ErrorCode.WALLET_NOT_FOUND, "Wallet " + walletId + " not found"));
    }

    public Optional<Wallet> findTreasury() {
        return walletRepository.findTreasury();
    }

    public Wallet getTreasury() {
        return findTreasury()
                .orElseThrow(() -> new DomainException(ErrorCode.WALLET_NOT_FOUND, "Treasury wallet is not initialized"));
    }

    public BigDecimal getBalance(Wallet wallet) {
        return blockchainGateway.getBalance(wallet.address());
    }

    /**
     * Комиссия за перевод amount с кошелька from на toAddress.
     * toAddress == null — на новый, ещё не созданный адрес (в Ethereum это дороже: создаётся аккаунт).
     */
    public BigDecimal estimateTransferFee(Wallet from, String toAddress, BigDecimal amount) {
        return blockchainGateway.estimateTransferFee(from.address(), toAddress, amount);
    }

    /**
     * Выполняет action, пока удерживается блокировка отправок для адреса кошелька.
     * Внутри можно безопасно проверить баланс и вызвать {@link #send(Wallet, String, BigDecimal)}.
     */
    public <T> T withSendLock(Wallet wallet, Supplier<T> action) {
        ReentrantLock lock = lockFor(wallet);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Отправляет ETH с кастодиального кошелька. Вызывать только внутри {@link #withSendLock}.
     * Приватный ключ расшифровывается только на время вызова и нигде не логируется.
     *
     * @return хэш транзакции
     */
    public String send(Wallet from, String toAddress, BigDecimal amount) {
        if (!lockFor(from).isHeldByCurrentThread()) {
            throw new IllegalStateException("send() must be called inside withSendLock() for " + from.address());
        }
        String privateKey = privateKeyCipher.decrypt(from.encryptedPrivateKey());
        String txHash = blockchainGateway.sendTransfer(privateKey, toAddress, amount);
        log.info("Sent {} ETH from {} to {}: tx {}", amount.toPlainString(), from.address(), toAddress, txHash);
        return txHash;
    }

    private ReentrantLock lockFor(Wallet wallet) {
        return sendLocks.computeIfAbsent(wallet.address().toLowerCase(Locale.ROOT), key -> new ReentrantLock());
    }
}
