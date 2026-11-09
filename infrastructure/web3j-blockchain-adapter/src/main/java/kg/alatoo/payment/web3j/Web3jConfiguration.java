package kg.alatoo.payment.web3j;

import kg.alatoo.payment.exceptions.BlockchainException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * Подключение к настоящей сети — только в профиле sepolia.
 * Сервисы не меняются: они получают тот же порт BlockchainGateway, только с другой реализацией (OCP, LSP).
 */
@Slf4j
@Configuration
@Profile("sepolia")
@EnableConfigurationProperties(Web3jProperties.class)
public class Web3jConfiguration {

    @Bean(destroyMethod = "shutdown")
    public Web3j web3j(Web3jProperties properties) {
        if (properties.getRpcUrl() == null || properties.getRpcUrl().isBlank()) {
            throw new IllegalStateException("ETH_RPC_URL is not set. Example: https://ethereum-sepolia-rpc.publicnode.com");
        }
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .build();
        log.info("Connecting to Ethereum RPC {} (chain id {})", properties.getRpcUrl(), properties.getChainId());
        return Web3j.build(new HttpService(properties.getRpcUrl(), httpClient));
    }

    @Bean
    public Web3jBlockchainGateway web3jBlockchainGateway(Web3j web3j, Web3jProperties properties) {
        return new Web3jBlockchainGateway(web3j, properties.getChainId(), properties.getGasPriceMultiplier(),
                properties.getExplorerUrl());
    }

    /**
     * При старте сверяем chain id узла с конфигом: подписать транзакцию для другой сети — частая ошибка.
     * Если узел недоступен — только предупреждаем (публичный RPC иногда «моргает»).
     */
    @Bean
    @Order(-10)
    public ApplicationRunner chainIdCheck(Web3jBlockchainGateway gateway, Web3jProperties properties) {
        return args -> {
            try {
                long nodeChainId = gateway.fetchChainId();
                if (nodeChainId != properties.getChainId()) {
                    throw new IllegalStateException("ETH_RPC_URL points to chain " + nodeChainId
                            + ", but ETH_CHAIN_ID is " + properties.getChainId());
                }
                log.info("Ethereum RPC is reachable, chain id {}", nodeChainId);
            } catch (BlockchainException e) {
                log.warn("Ethereum RPC is not reachable right now: {}", e.getMessage());
            }
        };
    }
}
