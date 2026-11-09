package kg.alatoo.payment.web3j;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Настройки подключения к Ethereum (префикс eth в application-sepolia.yml).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "eth")
public class Web3jProperties {
    /** RPC-узел, например https://ethereum-sepolia-rpc.publicnode.com (ETH_RPC_URL). */
    private String rpcUrl;
    /** Sepolia = 11155111 (ETH_CHAIN_ID). */
    private long chainId = 11155111L;
    /** Запас к цене газа, чтобы транзакция не застряла при росте цены. */
    private BigDecimal gasPriceMultiplier = new BigDecimal("1.2");
    private String explorerUrl = "https://sepolia.etherscan.io";
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);
}
