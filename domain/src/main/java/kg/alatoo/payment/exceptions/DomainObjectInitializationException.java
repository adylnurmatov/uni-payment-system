package kg.alatoo.payment.exceptions;

/**
 * Нарушен инвариант доменного объекта (ошибка программиста, а не пользователя).
 */
public class DomainObjectInitializationException extends RuntimeException {

    public DomainObjectInitializationException(String message) {
        super(message);
    }
}
