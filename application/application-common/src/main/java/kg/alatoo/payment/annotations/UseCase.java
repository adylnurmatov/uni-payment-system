package kg.alatoo.payment.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает реализацию сценария (use case). Application-слой не зависит от Spring:
 * бины находит boot-модуль через ComponentScan с фильтром по этой аннотации.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UseCase {
}
