package kg.alatoo.payment.config;

import kg.alatoo.payment.GetTreasuryPort;
import kg.alatoo.payment.GetTreasuryResult;
import kg.alatoo.payment.fake.FakeBlockchainGateway;
import kg.alatoo.payment.fake.FakeBlockchainProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Только для профиля fake: «кран» начисляет казне тестовые ETH при старте
 * (балансы fake-блокчейна живут в памяти и обнуляются после рестарта).
 * В профиле sepolia казну пополняют вручную через настоящий кран.
 */
@Slf4j
@Component
@Profile("fake")
@Order(10)
public class FakeTreasuryFaucet implements ApplicationRunner {
    private final GetTreasuryPort getTreasury;
    private final FakeBlockchainGateway fakeBlockchain;
    private final FakeBlockchainProperties properties;

    public FakeTreasuryFaucet(GetTreasuryPort getTreasury,
                              FakeBlockchainGateway fakeBlockchain,
                              FakeBlockchainProperties properties) {
        this.getTreasury = getTreasury;
        this.fakeBlockchain = fakeBlockchain;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        GetTreasuryResult treasury = getTreasury.execute();
        if (treasury.balance().signum() == 0 && properties.getTreasuryInitialBalance().signum() > 0) {
            fakeBlockchain.credit(treasury.address(), properties.getTreasuryInitialBalance());
            log.info("[fake] Treasury {} credited with {} test ETH",
                    treasury.address(), properties.getTreasuryInitialBalance().toPlainString());
        }
    }
}
