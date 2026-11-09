package kg.alatoo.payment;

/**
 * Сработал уникальный индекс по Idempotency-Key (платежи: merchant_id + key, возвраты: payment_id + key):
 * параллельный запрос с тем же ключом успел создать запись раньше.
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {

    public DuplicateIdempotencyKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
