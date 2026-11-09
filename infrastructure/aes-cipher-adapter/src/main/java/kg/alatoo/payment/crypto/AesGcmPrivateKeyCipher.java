package kg.alatoo.payment.crypto;

import kg.alatoo.payment.PrivateKeyCipher;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Шифрует приватные ключи кошельков алгоритмом AES-256-GCM.
 * <ul>
 *     <li>мастер-ключ — 32 байта из переменной окружения WALLET_ENCRYPTION_KEY (base64);</li>
 *     <li>для каждого шифрования новый случайный IV (12 байт) — одинаковые ключи дают разный шифртекст;</li>
 *     <li>GCM добавляет тег аутентичности (128 бит): подмена шифртекста в БД обнаруживается при расшифровке.</li>
 * </ul>
 * Формат в БД: base64(IV || ciphertext || tag).
 * <p>
 * ВАЖНО: потеря мастер-ключа = потеря доступа ко всем кошелькам.
 */
public class AesGcmPrivateKeyCipher implements PrivateKeyCipher {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey masterKey;
    private final SecureRandom random = new SecureRandom();

    public AesGcmPrivateKeyCipher(String base64MasterKey) {
        this.masterKey = new SecretKeySpec(decodeMasterKey(base64MasterKey), "AES");
    }

    @Override
    public String encrypt(String privateKeyHex) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(privateKeyHex.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt private key", e);
        }
    }

    @Override
    public String decrypt(String encryptedPrivateKey) {
        try {
            byte[] data = Base64.getDecoder().decode(encryptedPrivateKey);
            if (data.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Encrypted private key is too short");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(TAG_LENGTH_BITS, data, 0, IV_LENGTH_BYTES));
            byte[] plain = cipher.doFinal(data, IV_LENGTH_BYTES, data.length - IV_LENGTH_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // Чаще всего: WALLET_ENCRYPTION_KEY не тот, которым шифровались ключи в этой БД.
            // Сам шифртекст в сообщение не попадает.
            throw new IllegalStateException(
                    "Failed to decrypt private key: WALLET_ENCRYPTION_KEY does not match the one used for this database", e);
        }
    }

    private static byte[] decodeMasterKey(String base64MasterKey) {
        if (base64MasterKey == null || base64MasterKey.isBlank()) {
            throw new InvalidEncryptionKeyException("WALLET_ENCRYPTION_KEY is not set.");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64MasterKey.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidEncryptionKeyException("WALLET_ENCRYPTION_KEY is not valid base64.");
        }
        if (key.length != KEY_LENGTH_BYTES) {
            throw new InvalidEncryptionKeyException("WALLET_ENCRYPTION_KEY must be " + KEY_LENGTH_BYTES
                    + " bytes after base64 decoding, but was " + key.length + " bytes.");
        }
        return key;
    }
}
