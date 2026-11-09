package kg.alatoo.payment.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает общий сервис application-слоя, который переиспользуют несколько use case
 * (например, WalletService). Регистрируется в Spring так же, как {@link UseCase}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApplicationService {
}
