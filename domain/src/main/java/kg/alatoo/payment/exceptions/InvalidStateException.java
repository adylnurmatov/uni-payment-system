package kg.alatoo.payment.exceptions;

/**
 * Недопустимый переход статуса (например, возврат по ещё не подтверждённому платежу).
 */
public class InvalidStateException extends DomainException {

    public InvalidStateException(String message) {
        super(ErrorCode.INVALID_STATE, message);
    }
}
