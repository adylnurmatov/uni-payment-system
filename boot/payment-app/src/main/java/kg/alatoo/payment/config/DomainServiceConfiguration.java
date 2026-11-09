package kg.alatoo.payment.config;

import kg.alatoo.payment.ConfirmationSettings;
import kg.alatoo.payment.MerchantSettings;
import kg.alatoo.payment.services.PaymentStateMachine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;

/**
 * Бины для классов домена и application-слоя, которые сами не знают про Spring.
 */
@Configuration
public class DomainServiceConfiguration {

    @Bean
    public PaymentStateMachine paymentStateMachine() {
        return new PaymentStateMachine();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ConfirmationSettings confirmationSettings(@Value("${confirmation.timeout:10m}") Duration timeout) {
        return new ConfirmationSettings(timeout);
    }

    @Bean
    public MerchantSettings merchantSettings(@Value("${merchant.initial-funding:0.01}") BigDecimal initialFunding) {
        return new MerchantSettings(initialFunding);
    }
}
