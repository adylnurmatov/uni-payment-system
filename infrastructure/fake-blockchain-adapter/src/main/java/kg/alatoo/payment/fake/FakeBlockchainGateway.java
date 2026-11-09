package kg.alatoo.payment.fake;

import kg.alatoo.payment.BlockchainExplorer;
import kg.alatoo.payment.BlockchainGateway;
import kg.alatoo.payment.GeneratedKeyPair;
import kg.alatoo.payment.exceptions.BlockchainException;
import kg.alatoo.payment.models.TransactionStatus;
import kg.alatoo.payment.utils.Hashing;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory «блокчейн» для локальной разработки и тестов: без сети и без монет.
 * <ul>
 *     <li>адрес детерминированно выводится из приватного ключа (как в настоящем Ethereum);</li>
 *     <li>балансы хранятся в памяти (после рестарта обнуляются);</li>
 *     <li>перевод сразу списывает сумму + комиссию, а статус SUCCESS появляется через confirmationDelay
 *     — так видно честный статус PENDING.</li>
 * </ul>
 * Реализует тот же порт, что и Web3jBlockchainGateway (LSP), поэтому сервисы не знают, с чем работают.
 */
public class FakeBlockchainGateway implements BlockchainGateway, BlockchainExplorer {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    private final Map<String, FakeTransaction> transactions = new ConcurrentHashMap<>();
    private final BigDecimal transferFee;
    private final Duration confirmationDelay;
    private final Clock clock;

    public FakeBlockchainGateway(BigDecimal transferFee, Duration confirmationDelay, Clock clock) {
        this.transferFee = transferFee;
        this.confirmationDelay = confirmationDelay;
        this.clock = clock;
    }

    @Override
    public GeneratedKeyPair generateKeyPair() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        String privateKeyHex = HexFormat.of().formatHex(key);
        return new GeneratedKeyPair(addressOf(privateKeyHex), privateKeyHex);
    }

    @Override
    public BigDecimal getBalance(String address) {
        return balances.getOrDefault(normalize(address), BigDecimal.ZERO);
    }

    /** В fake-сети комиссия фиксированная и не зависит от получателя. */
    @Override
    public BigDecimal estimateTransferFee(String fromAddress, String toAddress, BigDecimal amount) {
        return transferFee;
    }

    @Override
    public synchronized String sendTransfer(String fromPrivateKey, String toAddress, BigDecimal amount) {
        String from = addressOf(fromPrivateKey);
        BigDecimal required = amount.add(transferFee);
        BigDecimal balance = getBalance(from);
        if (balance.compareTo(required) < 0) {
            // Так же отвечает настоящий узел Ethereum
            throw new BlockchainException("insufficient funds for gas * price + value: balance "
                    + balance.toPlainString() + ", required " + required.toPlainString());
        }
        balances.put(from, balance.subtract(required));
        balances.merge(normalize(toAddress), amount, BigDecimal::add);

        String txHash = "0x" + randomHex(32);
        transactions.put(txHash, new FakeTransaction(clock.instant(), null));
        return txHash;
    }

    @Override
    public TransactionStatus getTransactionStatus(String txHash) {
        FakeTransaction tx = transactions.get(txHash);
        if (tx == null) {
            // Как у настоящего узла: нет receipt — значит, пока PENDING (таймаут решит планировщик).
            return TransactionStatus.PENDING;
        }
        if (tx.forcedStatus() != null) {
            return tx.forcedStatus();
        }
        boolean mined = !clock.instant().isBefore(tx.sentAt().plus(confirmationDelay));
        return mined ? TransactionStatus.SUCCESS : TransactionStatus.PENDING;
    }

    @Override
    public String transactionUrl(String txHash) {
        return txHash == null ? null : "fake-explorer://tx/" + txHash;
    }

    @Override
    public String addressUrl(String address) {
        return address == null ? null : "fake-explorer://address/" + address;
    }

    // ---------- Методы только для fake-режима (не входят в порт) ----------

    /** «Кран»: начислить ETH на адрес. */
    public void credit(String address, BigDecimal amount) {
        balances.merge(normalize(address), amount, BigDecimal::add);
    }

    /** Для тестов: принудительно задать итоговый статус транзакции (например, FAILED = reverted). */
    public void forceStatus(String txHash, TransactionStatus status) {
        transactions.computeIfPresent(txHash, (hash, tx) -> new FakeTransaction(tx.sentAt(), status));
    }

    public static String addressOf(String privateKeyHex) {
        return "0x" + Hashing.sha256Hex(privateKeyHex).substring(0, 40);
    }

    private static String normalize(String address) {
        return address.toLowerCase(Locale.ROOT);
    }

    private static String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    private record FakeTransaction(Instant sentAt, TransactionStatus forcedStatus) {
    }
}
