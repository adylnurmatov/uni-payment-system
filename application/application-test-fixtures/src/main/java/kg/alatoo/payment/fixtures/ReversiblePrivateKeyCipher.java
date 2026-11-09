package kg.alatoo.payment.fixtures;

import kg.alatoo.payment.PrivateKeyCipher;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Тестовый «шифр» (base64), чтобы unit-тесты use case не зависели от AES-адаптера. */
public class ReversiblePrivateKeyCipher implements PrivateKeyCipher {

    @Override
    public String encrypt(String privateKeyHex) {
        return "test:" + Base64.getEncoder().encodeToString(privateKeyHex.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(String encryptedPrivateKey) {
        return new String(Base64.getDecoder().decode(encryptedPrivateKey.substring("test:".length())),
                StandardCharsets.UTF_8);
    }
}
