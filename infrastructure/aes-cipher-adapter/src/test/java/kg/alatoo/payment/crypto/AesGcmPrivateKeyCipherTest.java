package kg.alatoo.payment.crypto;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmPrivateKeyCipherTest {
    private static final String PRIVATE_KEY = "4c0883a69102937d6231471b5dbb6204fe5129617082792ae468d01a3f362318";

    private final AesGcmPrivateKeyCipher cipher = new AesGcmPrivateKeyCipher(randomKey());

    @Test
    void encryptThenDecryptReturnsOriginal() {
        String encrypted = cipher.encrypt(PRIVATE_KEY);

        assertThat(encrypted).doesNotContain(PRIVATE_KEY);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(PRIVATE_KEY);
    }

    @Test
    void sameInputGivesDifferentCiphertextBecauseOfRandomIv() {
        assertThat(cipher.encrypt(PRIVATE_KEY)).isNotEqualTo(cipher.encrypt(PRIVATE_KEY));
    }

    @Test
    void tamperedCiphertextIsRejected() {
        byte[] data = Base64.getDecoder().decode(cipher.encrypt(PRIVATE_KEY));
        data[data.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(data);

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anotherMasterKeyCannotDecrypt() {
        String encrypted = cipher.encrypt(PRIVATE_KEY);
        AesGcmPrivateKeyCipher other = new AesGcmPrivateKeyCipher(randomKey());

        assertThatThrownBy(() -> other.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining(encrypted);
    }

    @Test
    void missingKeyFailsFast() {
        assertThatThrownBy(() -> new AesGcmPrivateKeyCipher(""))
                .isInstanceOf(InvalidEncryptionKeyException.class)
                .hasMessageContaining("not set");
    }

    @Test
    void wrongLengthKeyFailsFast() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new AesGcmPrivateKeyCipher(shortKey))
                .isInstanceOf(InvalidEncryptionKeyException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void notBase64KeyFailsFast() {
        assertThatThrownBy(() -> new AesGcmPrivateKeyCipher("not base64 !!!"))
                .isInstanceOf(InvalidEncryptionKeyException.class);
    }

    private static String randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
