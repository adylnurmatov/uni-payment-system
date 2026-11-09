package kg.alatoo.payment;

import kg.alatoo.payment.annotations.ApplicationService;
import kg.alatoo.payment.annotations.UseCase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Точка входа. Application-слой не знает про Spring: use case и общие сервисы
 * регистрируются как бины здесь, через фильтр по аннотациям @UseCase и @ApplicationService.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = "kg.alatoo.payment",
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION,
                classes = {UseCase.class, ApplicationService.class})
)
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
