package kg.alatoo.payment;

/**
 * Сработал уникальный индекс (merchant_id, idempotency_key): параллельный запрос
 * с тем же ключом успел создать платёж раньше.
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {

    public DuplicateIdempotencyKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
