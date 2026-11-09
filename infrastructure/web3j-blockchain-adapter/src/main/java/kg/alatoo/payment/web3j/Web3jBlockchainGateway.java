package kg.alatoo.payment.web3j;

import kg.alatoo.payment.BlockchainExplorer;
import kg.alatoo.payment.BlockchainGateway;
import kg.alatoo.payment.GeneratedKeyPair;
import kg.alatoo.payment.exceptions.BlockchainException;
import kg.alatoo.payment.models.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реальная реализация {@link BlockchainGateway} для Ethereum (Sepolia) на web3j.
 * Все классы web3j остаются внутри этого адаптера: наружу уходят только BigDecimal, String
 * и доменные исключения ({@link BlockchainException}).
 *
 * <h3>Отправка без ожидания блока</h3>
 * Транзакция подписывается локально и отправляется через eth_sendRawTransaction. Метод сразу возвращает
 * хэш (статус PENDING), а подтверждение отслеживает планировщик через {@link #getTransactionStatus}.
 *
 * <h3>Nonce</h3>
 * У каждой транзакции адреса есть порядковый номер (nonce). Две транзакции с одинаковым nonce — одна
 * будет отклонена. Защита двухуровневая:
 * <ol>
 *     <li>WalletService отправляет с одного адреса строго последовательно (блокировка по адресу);</li>
 *     <li>здесь nonce = max(eth_getTransactionCount(pending) от узла, следующий локальный nonce).
 *     Локальный счётчик нужен, потому что публичный RPC — это балансировщик над несколькими узлами,
 *     и следующий запрос может попасть на узел, который ещё не видел нашу предыдущую pending-транзакцию.</li>
 * </ol>
 */
@Slf4j
public class Web3jBlockchainGateway implements BlockchainGateway, BlockchainExplorer {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Web3j web3j;
    private final long chainId;
    private final BigDecimal gasPriceMultiplier;
    private final BigDecimal gasLimitMultiplier;
    private final BigInteger fallbackGasLimit;
    private final String explorerUrl;
    private final Map<String, BigInteger> nextNonces = new ConcurrentHashMap<>();

    public Web3jBlockchainGateway(Web3j web3j, long chainId, BigDecimal gasPriceMultiplier,
                                  BigDecimal gasLimitMultiplier, BigInteger fallbackGasLimit, String explorerUrl) {
        this.web3j = web3j;
        this.chainId = chainId;
        this.gasPriceMultiplier = gasPriceMultiplier;
        this.gasLimitMultiplier = gasLimitMultiplier;
        this.fallbackGasLimit = fallbackGasLimit;
        this.explorerUrl = explorerUrl.endsWith("/") ? explorerUrl.substring(0, explorerUrl.length() - 1) : explorerUrl;
    }

    @Override
    public GeneratedKeyPair generateKeyPair() {
        try {
            ECKeyPair keyPair = Keys.createEcKeyPair();
            String address = "0x" + Keys.getAddress(keyPair);
            String privateKeyHex = Numeric.toHexStringNoPrefixZeroPadded(keyPair.getPrivateKey(), 64);
            return new GeneratedKeyPair(address, privateKeyHex);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to generate Ethereum key pair", e);
        }
    }

    /**
     * Баланс по блоку "pending": учитывает наши ещё не подтверждённые исходящие транзакции,
     * чтобы два платежа подряд не прошли проверку баланса на одни и те же деньги.
     */
    @Override
    public BigDecimal getBalance(String address) {
        BigInteger wei = call(() -> web3j.ethGetBalance(address, DefaultBlockParameterName.PENDING).send(),
                "get balance of " + address).getBalance();
        return Convert.fromWei(new BigDecimal(wei), Convert.Unit.ETHER);
    }

    /**
     * Максимальная комиссия = лимит газа × цена газа (оба с запасом).
     * Лимит газа спрашиваем у сети (eth_estimateGas), а не берём «классические» 21000: в текущей Sepolia
     * перевод на ещё не существующий адрес стоит ~207 000 газа (создание аккаунта), а на существующий — 12 000.
     */
    @Override
    public BigDecimal estimateTransferFee(String fromAddress, String toAddress, BigDecimal amount) {
        BigInteger feeWei = gasLimitFor(fromAddress, toAddress, toWei(amount)).multiply(currentGasPrice());
        return Convert.fromWei(new BigDecimal(feeWei), Convert.Unit.ETHER);
    }

    @Override
    public String sendTransfer(String fromPrivateKey, String toAddress, BigDecimal amount) {
        Credentials credentials = Credentials.create(fromPrivateKey);
        String from = credentials.getAddress().toLowerCase(Locale.ROOT);
        BigInteger valueWei = toWei(amount);
        BigInteger gasPrice = currentGasPrice();
        BigInteger gasLimit = gasLimitFor(from, toAddress, valueWei);
        BigInteger nonce = nextNonce(from);

        RawTransaction transaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit, toAddress, valueWei);
        String signed = Numeric.toHexString(TransactionEncoder.signMessage(transaction, chainId, credentials));

        EthSendTransaction response;
        try {
            response = web3j.ethSendRawTransaction(signed).send();
        } catch (IOException | RuntimeException e) {
            // Неизвестно, дошла ли транзакция до узла — сбрасываем локальный nonce, в следующий раз спросим узел
            nextNonces.remove(from);
            throw new BlockchainException("Ethereum RPC is unavailable (send transaction): " + describe(e), e);
        }
        if (response.hasError()) {
            nextNonces.remove(from);
            throw new BlockchainException("Node rejected transaction: " + response.getError().getMessage());
        }
        nextNonces.put(from, nonce.add(BigInteger.ONE));
        log.info("Transaction {} sent from {} (nonce {}, gasLimit {}, gasPrice {} wei)",
                response.getTransactionHash(), from, nonce, gasLimit, gasPrice);
        return response.getTransactionHash();
    }

    @Override
    public TransactionStatus getTransactionStatus(String txHash) {
        Optional<TransactionReceipt> receipt = call(() -> web3j.ethGetTransactionReceipt(txHash).send(),
                "get receipt of " + txHash).getTransactionReceipt();
        if (receipt.isEmpty()) {
            return TransactionStatus.PENDING;
        }
        return receipt.get().isStatusOK() ? TransactionStatus.SUCCESS : TransactionStatus.FAILED;
    }

    @Override
    public String transactionUrl(String txHash) {
        return txHash == null ? null : explorerUrl + "/tx/" + txHash;
    }

    @Override
    public String addressUrl(String address) {
        return address == null ? null : explorerUrl + "/address/" + address;
    }

    /** Chain ID, который сообщает узел (для проверки конфигурации при старте). */
    public long fetchChainId() {
        return call(() -> web3j.ethChainId().send(), "get chain id").getChainId().longValueExact();
    }

    private BigInteger nextNonce(String address) {
        BigInteger fromNode = call(() -> web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send(),
                "get nonce of " + address).getTransactionCount();
        BigInteger local = nextNonces.get(address);
        return local == null ? fromNode : fromNode.max(local);
    }

    /**
     * Лимит газа для перевода = оценка сети × запас (по умолчанию +20%). Неизрасходованный газ не списывается,
     * запас нужен, чтобы транзакция не упала по out-of-gas.
     * Для оценки достаточно 1 wei: цена газа зависит от того, есть ли перевод (value &gt; 0) и существует ли получатель,
     * но не от размера суммы. Так оценка работает, даже если у отправителя пока не хватает денег на саму сумму.
     */
    private BigInteger gasLimitFor(String from, String to, BigInteger valueWei) {
        String target = to != null ? to : randomNewAddress();
        BigInteger probeValue = valueWei.signum() > 0 ? BigInteger.ONE : BigInteger.ZERO;
        Transaction probe = Transaction.createEtherTransaction(from, null, null, null, target, probeValue);
        BigInteger estimate;
        try {
            estimate = call(() -> web3j.ethEstimateGas(probe).send(), "estimate gas").getAmountUsed();
        } catch (BlockchainException e) {
            if (String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT).contains("insufficient funds")) {
                // У отправителя совсем нет денег — оценить нельзя; берём консервативный лимит,
                // проверка баланса дальше всё равно вернёт понятный INSUFFICIENT_FUNDS
                return fallbackGasLimit;
            }
            throw e;
        }
        return new BigDecimal(estimate).multiply(gasLimitMultiplier).setScale(0, RoundingMode.CEILING).toBigInteger();
    }

    /** Случайный адрес, которого точно нет в сети: так оценивается перевод на ещё не созданный кошелёк. */
    private static String randomNewAddress() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return Numeric.toHexString(bytes);
    }

    private BigInteger currentGasPrice() {
        BigInteger gasPrice = call(() -> web3j.ethGasPrice().send(), "get gas price").getGasPrice();
        return new BigDecimal(gasPrice).multiply(gasPriceMultiplier).setScale(0, RoundingMode.CEILING).toBigInteger();
    }

    private static BigInteger toWei(BigDecimal amountEth) {
        try {
            return Convert.toWei(amountEth, Convert.Unit.ETHER).toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new BlockchainException("Amount " + amountEth.toPlainString() + " ETH is smaller than 1 wei");
        }
    }

    /** Вызов RPC: сетевые ошибки и ошибки узла превращаются в доменный BlockchainException. */
    private static <T extends Response<?>> T call(RpcCall<T> rpcCall, String what) {
        T response;
        try {
            response = rpcCall.execute();
        } catch (IOException | RuntimeException e) {
            // RuntimeException: например, ClientConnectionException на HTTP 429 (лимит запросов публичного RPC)
            throw new BlockchainException("Ethereum RPC is unavailable (" + what + "): " + describe(e), e);
        }
        if (response.hasError()) {
            throw new BlockchainException("Ethereum RPC error (" + what + "): " + response.getError().getMessage());
        }
        return response;
    }

    private static String describe(Exception e) {
        String message = String.valueOf(e.getMessage());
        return message.contains("429") ? "RPC rate limit exceeded (HTTP 429), use an Infura/Alchemy key" : message;
    }

    @FunctionalInterface
    private interface RpcCall<T> {
        T execute() throws IOException;
    }
}
