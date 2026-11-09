package kg.alatoo.payment;

/**
 * Шифрование приватных ключей кошельков перед записью в БД.
 * Реализация: AesGcmPrivateKeyCipher (AES-256-GCM, мастер-ключ из WALLET_ENCRYPTION_KEY).
 */
public interface PrivateKeyCipher {

    String encrypt(String privateKeyHex);

    String decrypt(String encryptedPrivateKey);
}
