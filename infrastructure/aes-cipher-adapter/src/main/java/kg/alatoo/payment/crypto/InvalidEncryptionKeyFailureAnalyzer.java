package kg.alatoo.payment.crypto;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Превращает падение старта из-за WALLET_ENCRYPTION_KEY в понятное сообщение вместо длинного stack trace.
 * Зарегистрирован в META-INF/spring.factories.
 */
public class InvalidEncryptionKeyFailureAnalyzer extends AbstractFailureAnalyzer<InvalidEncryptionKeyException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, InvalidEncryptionKeyException cause) {
        return new FailureAnalysis(
                cause.getMessage() + " The application cannot start without a valid wallet encryption key.",
                """
                        Generate a 32-byte key and put it into .env (or the environment):
                            openssl rand -base64 32
                            WALLET_ENCRYPTION_KEY=<output>
                        WARNING: use the SAME key that was used for the existing database,
                        otherwise wallets stored in it cannot be decrypted.""",
                cause);
    }
}
