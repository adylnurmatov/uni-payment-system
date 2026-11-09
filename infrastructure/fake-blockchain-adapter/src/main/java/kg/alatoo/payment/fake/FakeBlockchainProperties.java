package kg.alatoo.payment.fake;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Настройки fake-блокчейна (префикс fake-blockchain в application-fake.yml).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "fake-blockchain")
public class FakeBlockchainProperties {
    /** Комиссия за перевод: 21000 газа * 1 gwei = 0.000021 ETH. */
    private BigDecimal transferFee = new BigDecimal("0.000021");
    /** Через сколько транзакция «попадает в блок». */
    private Duration confirmationDelay = Duration.ofSeconds(20);
    /** Сколько ETH начислить казне при старте, если она пустая. */
    private BigDecimal treasuryInitialBalance = new BigDecimal("100");
}
