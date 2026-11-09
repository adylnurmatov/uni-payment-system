package kg.alatoo.payment.crypto;

/**
 * WALLET_ENCRYPTION_KEY не задан или неверный — приложение не должно стартовать.
 */
public class InvalidEncryptionKeyException extends RuntimeException {

    public InvalidEncryptionKeyException(String message) {
        super(message);
    }
}
