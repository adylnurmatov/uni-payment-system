package kg.alatoo.payment.fake;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

/**
 * Регистрирует FakeBlockchainGateway только в профиле fake.
 * Один бин реализует оба порта: BlockchainGateway и BlockchainExplorer.
 */
@Configuration
@Profile("fake")
@EnableConfigurationProperties(FakeBlockchainProperties.class)
public class FakeBlockchainConfiguration {

    @Bean
    public FakeBlockchainGateway fakeBlockchainGateway(FakeBlockchainProperties properties, Clock clock) {
        return new FakeBlockchainGateway(properties.getTransferFee(), properties.getConfirmationDelay(), clock);
    }
}
