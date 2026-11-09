package kg.alatoo.payment.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: /swagger-ui.html. Кнопка "Authorize" принимает X-Api-Key мерчанта
 * и логин/пароль администратора (Basic Auth).
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Crypto Payment Service",
        version = "1.0",
        description = "Учебная платёжная система на тестовой сети Ethereum Sepolia"))
@SecuritySchemes({
        @SecurityScheme(name = OpenApiConfiguration.MERCHANT_API_KEY, type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER, paramName = Headers.API_KEY),
        @SecurityScheme(name = OpenApiConfiguration.ADMIN_BASIC, type = SecuritySchemeType.HTTP, scheme = "basic")
})
public class OpenApiConfiguration {
    public static final String MERCHANT_API_KEY = "merchantApiKey";
    public static final String ADMIN_BASIC = "adminBasic";
}
