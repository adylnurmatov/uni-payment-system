package kg.alatoo.payment.exceptions;

import lombok.Getter;

/**
 * Базовое бизнес-исключение. Несёт {@link ErrorCode}, по которому строится ответ API.
 */
@Getter
public class DomainException extends RuntimeException {
    private final ErrorCode code;

    public DomainException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public DomainException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
