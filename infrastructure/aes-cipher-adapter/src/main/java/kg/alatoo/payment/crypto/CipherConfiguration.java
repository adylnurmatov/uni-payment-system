package kg.alatoo.payment.crypto;

import kg.alatoo.payment.PrivateKeyCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CipherConfiguration {

    /** Бин создаётся при старте: неверный ключ → приложение не стартует. */
    @Bean
    public PrivateKeyCipher privateKeyCipher(@Value("${wallet.encryption-key:}") String encryptionKey) {
        return new AesGcmPrivateKeyCipher(encryptionKey);
    }
}
