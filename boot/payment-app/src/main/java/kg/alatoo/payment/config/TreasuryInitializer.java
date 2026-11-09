package kg.alatoo.payment.config;

import kg.alatoo.payment.InitTreasuryPort;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * При старте создаёт казну, если её ещё нет.
 */
@Component
@Order(0)
public class TreasuryInitializer implements ApplicationRunner {
    private final InitTreasuryPort initTreasury;

    public TreasuryInitializer(InitTreasuryPort initTreasury) {
        this.initTreasury = initTreasury;
    }

    @Override
    public void run(ApplicationArguments args) {
        initTreasury.execute();
    }
}
