package kg.alatoo.payment;

import kg.alatoo.payment.exceptions.DomainException;
import kg.alatoo.payment.exceptions.ErrorCode;

/**
 * Проверка заголовка Idempotency-Key (общая для платежей и возвратов).
 */
public final class IdempotencyKeys {
    public static final int MAX_LENGTH = 255;

    private IdempotencyKeys() {
    }

    public static String require(String key) {
        if (key == null || key.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header is required");
        }
        String trimmed = key.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR,
                    "Idempotency-Key must be at most " + MAX_LENGTH + " characters");
        }
        return trimmed;
    }
}
